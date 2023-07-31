/*
 * Copyright (c) 2023-2023 Lunabee Studio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package studio.lunabee.doubleratchet

import studio.lunabee.doubleratchet.crypto.DoubleRatchetKeyRepository
import studio.lunabee.doubleratchet.model.AsymmetricKeyPair
import studio.lunabee.doubleratchet.model.Conversation
import studio.lunabee.doubleratchet.model.DRChainKey
import studio.lunabee.doubleratchet.model.DRMessageKey
import studio.lunabee.doubleratchet.model.DRPublicKey
import studio.lunabee.doubleratchet.model.DRRootKey
import studio.lunabee.doubleratchet.model.DRSharedSecret
import studio.lunabee.doubleratchet.model.DoubleRatchetError
import studio.lunabee.doubleratchet.model.DoubleRatchetUUID
import studio.lunabee.doubleratchet.model.InvitationData
import studio.lunabee.doubleratchet.model.MessageHeader
import studio.lunabee.doubleratchet.model.SendMessageData
import studio.lunabee.doubleratchet.model.createRandomUUID
import studio.lunabee.doubleratchet.storage.DoubleRatchetLocalDatasource

/**
 * Core engine
 */
class DoubleRatchetEngine(
    private val doubleRatchetLocalDatasource: DoubleRatchetLocalDatasource,
    private val doubleRatchetKeyRepository: DoubleRatchetKeyRepository,
) {

    /**
     * Generate the data needed to create an invitation to start a new conversation
     *
     * @param sharedSalt An initial shared salt
     * @param newConversationId The id to be used for the conversation
     *
     * @return the invitation to send
     */
    suspend fun createInvitation(
        sharedSalt: DRSharedSecret,
        newConversationId: DoubleRatchetUUID = createRandomUUID(),
    ): InvitationData {
        if (sharedSalt.value.size != doubleRatchetKeyRepository.rootKeyByteSize) {
            throw DoubleRatchetError(DoubleRatchetError.Type.SharedSaltWrongSize)
        }
        val keyPair: AsymmetricKeyPair = doubleRatchetKeyRepository.generateKeyPair()
        val conversation = Conversation.createNew(
            id = newConversationId,
            personalKeyPair = keyPair,
            initialRootKey = DRRootKey(sharedSalt.value.copyOf()),
        )
        saveAndDestroy(conversation)
        return InvitationData(
            conversationId = newConversationId,
            publicKey = keyPair.publicKey,
        )
    }

    /**
     * Generate the data needed to start a new conversation from an invitation
     *
     * @param contactPublicKey The public key coming from the invitation
     * @param sharedSalt An initial shared salt
     * @param newConversationId The id to be used for the conversation
     *
     * @return the conversation id
     */
    suspend fun createNewConversationFromInvitation(
        contactPublicKey: DRPublicKey,
        sharedSalt: DRSharedSecret,
        newConversationId: DoubleRatchetUUID = createRandomUUID(),
    ): DoubleRatchetUUID {
        if (sharedSalt.value.size != doubleRatchetKeyRepository.rootKeyByteSize) {
            throw DoubleRatchetError(DoubleRatchetError.Type.SharedSaltWrongSize)
        }
        val keyPair: AsymmetricKeyPair = doubleRatchetKeyRepository.generateKeyPair()
        val sharedSecret = doubleRatchetKeyRepository.createDiffieHellmanSharedSecret(contactPublicKey, keyPair.privateKey)
        val rootKey = DRRootKey(sharedSalt.value.copyOf())
        val keyRootPair = doubleRatchetKeyRepository.deriveRootKeys(rootKey, sharedSecret)
        val conversation = Conversation.createFromInvitation(
            id = newConversationId,
            personalKeyPair = keyPair,
            rootKey = keyRootPair.rootKey,
            sendingChainKey = keyRootPair.chainKey,
        )
        saveAndDestroy(conversation)

        return newConversationId
    }

    /**
     * Generate new message header to attach to the message you want to send, and the message key to use to encrypt the message
     *
     * @param conversationId The id of the associated conversation
     */
    suspend fun getSendData(conversationId: DoubleRatchetUUID): SendMessageData {
        val conversation = doubleRatchetLocalDatasource.getConversation(conversationId)
            ?: throw DoubleRatchetError(DoubleRatchetError.Type.ConversationNotFound)
        val sendingChainKey = conversation.sendingChainKey
            ?: throw DoubleRatchetError(DoubleRatchetError.Type.ConversationNotSetup)

        val messageKey = doubleRatchetKeyRepository.deriveChainKeys(
            chainKey = sendingChainKey,
            outChainKey = sendingChainKey,
        ).messageKey

        val messageData = SendMessageData(
            messageHeader = MessageHeader(
                messageNumber = conversation.nextMessageNumber,
                sequenceNumber = conversation.nextSequenceNumber,
                publicKey = conversation.personalKeyPair.publicKey,
            ),
            messageKey = messageKey,
        )

        conversation.apply {
            this.nextMessageNumber = conversation.nextMessageNumber.inc()
            this.nextSequenceNumber = conversation.nextSequenceNumber.inc()
        }
        saveAndDestroy(conversation)

        return messageData
    }

    /**
     * Retrieve the key associated to the received message
     *
     * @param messageHeader The header of the received message
     * @param conversationId The id of the associated conversation
     *
     * @return the key needed to decrypt the message attached to the messageHeader
     */
    suspend fun getReceiveKey(
        messageHeader: MessageHeader,
        conversationId: DoubleRatchetUUID,
    ): DRMessageKey {
        val conversation = doubleRatchetLocalDatasource.getConversation(conversationId)
            ?: throw DoubleRatchetError(DoubleRatchetError.Type.ConversationNotFound)
        if (conversation.rootKey == null) {
            throw DoubleRatchetError(DoubleRatchetError.Type.ConversationNotSetup)
        }

        val isMessageKeyAlreadyGenerated = conversation.receivedLastMessageNumber?.let { messageCount ->
            messageCount >= messageHeader.messageNumber
        } ?: false

        return if (isMessageKeyAlreadyGenerated) {
            conversation.destroy()
            popStoredMessageKey(messageHeader, conversationId)
        } else {
            computeNextMessageKeyAndUpdateConversation(messageHeader, conversation)
        }
    }

    private suspend fun popStoredMessageKey(
        messageHeader: MessageHeader,
        conversationId: DoubleRatchetUUID,
    ): DRMessageKey {
        val messageKeyId = getMessageKeyId(conversationId, messageHeader.messageNumber)
        return doubleRatchetLocalDatasource.popMessageKey(messageKeyId)
            ?: throw DoubleRatchetError(DoubleRatchetError.Type.MessageKeyNotFound)
    }

    private suspend fun computeNextMessageKeyAndUpdateConversation(
        messageHeader: MessageHeader,
        conversation: Conversation,
    ): DRMessageKey {
        val lastMessageNumber = conversation.receivedLastMessageNumber

        val newSequenceMessageNumber: Int? = if (messageHeader.publicKey.contentEquals(conversation.lastContactPublicKey)) {
            null
        } else {
            messageHeader.messageNumber - messageHeader.sequenceNumber
        }

        var messageNumber = lastMessageNumber?.inc() ?: 0
        val messageKey = DRMessageKey.empty(doubleRatchetKeyRepository.messageKeyByteSize)
        conversation.receiveChainKey = conversation.receiveChainKey ?: DRChainKey.empty(doubleRatchetKeyRepository.chainKeyByteSize)
        while (messageNumber <= messageHeader.messageNumber) {
            if (messageNumber == newSequenceMessageNumber) {
                receiveNewSequenceMessage(messageHeader.publicKey, conversation, messageKey)
                updateConversationForNextSend(messageHeader, conversation)
            } else {
                receiveCurrentSequenceMessage(conversation, messageKey)
            }

            if (messageNumber != messageHeader.messageNumber) {
                doubleRatchetLocalDatasource.saveMessageKey(
                    id = getMessageKeyId(conversation.id, messageNumber),
                    key = messageKey,
                )
            }
            messageNumber++
        }
        saveAndDestroy(conversation)
        return messageKey
    }

    private suspend fun receiveCurrentSequenceMessage(
        conversation: Conversation,
        messageKey: DRMessageKey,
    ) {
        doubleRatchetKeyRepository.deriveChainKeys(
            chainKey = conversation.receiveChainKey!!,
            outChainKey = conversation.receiveChainKey!!,
            outMessageKey = messageKey,
        )
        conversation.apply {
            receivedLastMessageNumber = conversation.receivedLastMessageNumber?.inc() ?: 1
        }
    }

    private suspend fun receiveNewSequenceMessage(
        publicKey: DRPublicKey,
        conversation: Conversation,
        messageKey: DRMessageKey,
    ) {
        val sharedSecret = doubleRatchetKeyRepository.createDiffieHellmanSharedSecret(
            publicKey = publicKey,
            privateKey = conversation.personalKeyPair.privateKey,
        )
        doubleRatchetKeyRepository.deriveRootKeys(
            rootKey = conversation.rootKey!!,
            sharedSecret = sharedSecret,
            outRootKey = conversation.rootKey!!,
            outChainKey = conversation.receiveChainKey!!,
        )
        sharedSecret.destroy()
        doubleRatchetKeyRepository.deriveChainKeys(
            chainKey = conversation.receiveChainKey!!,
            outChainKey = conversation.receiveChainKey!!,
            outMessageKey = messageKey,
        )

        conversation.apply {
            this.lastContactPublicKey = publicKey
            this.receivedLastMessageNumber = conversation.receivedLastMessageNumber?.inc() ?: 0
        }
    }

    private suspend fun updateConversationForNextSend(
        messageHeader: MessageHeader,
        conversation: Conversation,
    ) {
        val newKeyPair = doubleRatchetKeyRepository.generateKeyPair()
        val sharedSecret = doubleRatchetKeyRepository.createDiffieHellmanSharedSecret(
            messageHeader.publicKey,
            newKeyPair.privateKey,
        )
        val derivedKeyPair = doubleRatchetKeyRepository.deriveRootKeys(conversation.rootKey!!, sharedSecret)
        sharedSecret.destroy()

        conversation.apply {
            this.sendingChainKey = derivedKeyPair.chainKey
            this.rootKey = derivedKeyPair.rootKey
            this.personalKeyPair = newKeyPair
            this.nextMessageNumber = conversation.nextMessageNumber
            this.nextSequenceNumber = 0
        }
    }

    private fun getMessageKeyId(conversationId: DoubleRatchetUUID, messageNumber: Int): String {
        return "${conversationId.uuidString()} - $messageNumber"
    }

    private suspend fun saveAndDestroy(conversation: Conversation) {
        try {
            doubleRatchetLocalDatasource.saveOrUpdateConversation(conversation)
        } finally {
            conversation.destroy()
        }
    }
}
