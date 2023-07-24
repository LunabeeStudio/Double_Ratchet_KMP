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
import studio.lunabee.doubleratchet.model.DRMessageKey
import studio.lunabee.doubleratchet.model.DRPublicKey
import studio.lunabee.doubleratchet.model.DRRootKey
import studio.lunabee.doubleratchet.model.DRSharedSecret
import studio.lunabee.doubleratchet.model.DerivedKeyMessagePair
import studio.lunabee.doubleratchet.model.DerivedKeyRootPair
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
     * @param newConversationId The id to be used for the conversation
     *
     * @return the conversation id
     */
    suspend fun createInvitation(newConversationId: DoubleRatchetUUID = createRandomUUID()): InvitationData {
        val keyPair: AsymmetricKeyPair = doubleRatchetKeyRepository.generateKeyPair()
        val conversation = Conversation.createNew(
            id = newConversationId,
            personalKeyPair = keyPair,
        )
        doubleRatchetLocalDatasource.saveOrUpdateConversation(conversation)
        keyPair.privateKey.destroy()
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
        val keyPair: AsymmetricKeyPair = doubleRatchetKeyRepository.generateKeyPair()
        val sharedSecret = doubleRatchetKeyRepository.createDiffieHellmanSharedSecret(contactPublicKey, keyPair.privateKey)
        val rootKey = DRRootKey(sharedSalt.value) // Initial root key is the shared secret
        val keyRootPair = doubleRatchetKeyRepository.deriveRootKeys(rootKey, sharedSecret)
        val conversation = Conversation.createFromInvitation(
            id = newConversationId,
            personalKeyPair = keyPair,
            rootKey = keyRootPair.rootKey,
            sendingChainKey = keyRootPair.chainKey,
        )

        try {
            doubleRatchetLocalDatasource.saveOrUpdateConversation(conversation)
        } finally {
            keyRootPair.rootKey.destroy()
            keyRootPair.chainKey.destroy()
            keyPair.privateKey.destroy()
            keyPair.publicKey.destroy()
            contactPublicKey.destroy()
        }

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

        val derivedKeyPair = doubleRatchetKeyRepository.deriveChainKeys(sendingChainKey)

        val messageData = SendMessageData(
            messageHeader = MessageHeader(
                messageNumber = conversation.nextMessageNumber,
                sequenceNumber = conversation.nextSequenceNumber,
                publicKey = conversation.personalKeyPair.publicKey,
            ),
            messageKey = derivedKeyPair.messageKey,
        )

        conversation.apply {
            this.sendingChainKey = derivedKeyPair.chainKey // TODO optim already updated by deriveChainKeys
            this.nextMessageNumber = conversation.nextMessageNumber.inc()
            this.nextSequenceNumber = conversation.nextSequenceNumber.inc()
        }
        doubleRatchetLocalDatasource.saveOrUpdateConversation(conversation)
        derivedKeyPair.chainKey.destroy()

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

        return try {
            if (isMessageKeyAlreadyGenerated) {
                popStoredMessageKey(messageHeader, conversationId)
            } else {
                computeNextMessageKeyAndUpdateConversation(messageHeader, conversation)
            }
        } finally {
            conversation.destroy()
        }
    }

    /**
     * Finalize the initialization of the conversation and retrieve the key associated to the received message
     *
     * @param messageHeader The header of the received message
     * @param conversationId The id of the associated conversation
     * @param sharedSalt The initial shared salt used by the contact in [createNewConversationFromInvitation]
     *
     * @return the key needed to decrypt the message attached to the messageHeader
     */
    suspend fun getFirstReceiveKey(
        messageHeader: MessageHeader,
        conversationId: DoubleRatchetUUID,
        sharedSalt: DRSharedSecret,
    ): DRMessageKey {
        val conversation = doubleRatchetLocalDatasource.getConversation(conversationId)
            ?: throw DoubleRatchetError(DoubleRatchetError.Type.ConversationNotFound)
        if (conversation.rootKey != null) {
            throw DoubleRatchetError(DoubleRatchetError.Type.ConversationAlreadySetup)
        }
        conversation.rootKey = DRRootKey(sharedSalt.value) // Initial root key is the shared salt
        return try {
            computeNextMessageKeyAndUpdateConversation(messageHeader, conversation)
        } finally {
            conversation.destroy()
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

        val newSequenceMessageNumber: UInt? = if (messageHeader.publicKey.contentEquals(conversation.lastContactPublicKey)) {
            null
        } else {
            messageHeader.messageNumber - messageHeader.sequenceNumber
        }

        var messageNumber = lastMessageNumber?.inc() ?: 0u
        var messageKey: DRMessageKey? = null
        val derivedKeyPair = DerivedKeyMessagePair.empty()
        while (messageKey == null) {
            if (messageNumber == newSequenceMessageNumber) {
                receiveNewSequenceMessage(messageHeader.publicKey, conversation, derivedKeyPair)
                updateConversationForNextSend(messageHeader, conversation)
            } else {
                receiveOldSequenceMessageAndUpdateConversation(conversation, derivedKeyPair)
            }

            doubleRatchetLocalDatasource.saveOrUpdateConversation(conversation)

            if (messageNumber == messageHeader.messageNumber) {
                messageKey = derivedKeyPair.messageKey
            } else {
                doubleRatchetLocalDatasource.saveMessageKey(
                    id = getMessageKeyId(conversation.id, messageNumber),
                    key = derivedKeyPair.messageKey,
                )
                messageNumber++
            }
        }

        return messageKey
    }

    private suspend fun receiveOldSequenceMessageAndUpdateConversation(
        conversation: Conversation,
        derivedKeyPair: DerivedKeyMessagePair,
    ) {
        doubleRatchetKeyRepository.deriveChainKeys(conversation.receiveChainKey!!, derivedKeyPair)
        conversation.apply {
            receiveChainKey = derivedKeyPair.chainKey
            receivedLastMessageNumber = conversation.receivedLastMessageNumber?.inc() ?: 1u
        }
        doubleRatchetLocalDatasource.saveOrUpdateConversation(conversation)
    }

    private suspend fun receiveNewSequenceMessage(
        publicKey: DRPublicKey,
        conversation: Conversation,
        derivedKeyPair: DerivedKeyMessagePair,
    ) {
        val sharedSecret = doubleRatchetKeyRepository.createDiffieHellmanSharedSecret(
            publicKey = publicKey,
            privateKey = conversation.personalKeyPair.privateKey,
        )
        doubleRatchetKeyRepository.deriveRootKeys(
            rootKey = conversation.rootKey!!,
            sharedSecret = sharedSecret,
            out = DerivedKeyRootPair(conversation.rootKey!!, derivedKeyPair.chainKey)
        )
        sharedSecret.destroy()
        doubleRatchetKeyRepository.deriveChainKeys(
            chainKey = derivedKeyPair.chainKey,
            out = derivedKeyPair,
        )

        conversation.apply {
            this.receiveChainKey = derivedKeyPair.chainKey
            this.lastContactPublicKey = publicKey
            this.receivedLastMessageNumber = conversation.receivedLastMessageNumber?.inc() ?: 0u
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
            this.nextSequenceNumber = 0u
        }
    }

    private fun getMessageKeyId(conversationId: DoubleRatchetUUID, messageNumber: UInt): String {
        return "${conversationId.uuidString()} - $messageNumber"
    }
}
