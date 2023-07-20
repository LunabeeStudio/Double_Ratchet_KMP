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
import studio.lunabee.doubleratchet.model.DRChainKey
import studio.lunabee.doubleratchet.model.Conversation
import studio.lunabee.doubleratchet.model.DerivedKeyPair
import studio.lunabee.doubleratchet.model.DoubleRatchetError
import studio.lunabee.doubleratchet.model.DoubleRatchetUUID
import studio.lunabee.doubleratchet.model.InvitationData
import studio.lunabee.doubleratchet.model.MessageConversationCounter
import studio.lunabee.doubleratchet.model.MessageHeader
import studio.lunabee.doubleratchet.model.DRMessageKey
import studio.lunabee.doubleratchet.model.DRPublicKey
import studio.lunabee.doubleratchet.model.SendMessageData
import studio.lunabee.doubleratchet.model.DRSharedSecret
import studio.lunabee.doubleratchet.model.createRandomUUID
import studio.lunabee.doubleratchet.model.use
import studio.lunabee.doubleratchet.storage.DoubleRatchetLocalDatasource

class DoubleRatchetEngine(
    private val doubleRatchetLocalDatasource: DoubleRatchetLocalDatasource,
    private val doubleRatchetKeyRepository: DoubleRatchetKeyRepository,
) {

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
     * @return the provided or generated id assign to the conversation created
     */
    suspend fun createNewConversationFromInvitation(
        contactPublicKey: DRPublicKey,
        newConversationId: DoubleRatchetUUID = createRandomUUID(),
    ): DoubleRatchetUUID {
        val keyPair: AsymmetricKeyPair = doubleRatchetKeyRepository.generateKeyPair()
        val sendChainKey = doubleRatchetKeyRepository.generateChainKey()
        doubleRatchetLocalDatasource.saveChainKey(id = newConversationId, sendChainKey)
        val receiveChainKey: DRChainKey = doubleRatchetKeyRepository.deriveKey(sendChainKey).chainKey
        val conversation = Conversation.createFromInvitation(
            id = newConversationId,
            personalKeyPair = keyPair,
            sendChainKey = sendChainKey,
            receiveChainKey = receiveChainKey,
            contactPublicKey = contactPublicKey,
        )

        try {
            doubleRatchetLocalDatasource.saveOrUpdateConversation(conversation)
        } finally {
            sendChainKey.destroy()
            keyPair.privateKey.destroy()
            keyPair.publicKey.destroy()
            contactPublicKey.destroy()
        }

        return newConversationId
    }

    /**
     * Generate new message header to attach to the message you want to send, and the message key to use to encrypt the message
     */
    suspend fun getSendData(conversationId: DoubleRatchetUUID): SendMessageData {
        val conversation = doubleRatchetLocalDatasource.getConversation(conversationId)
            ?: throw DoubleRatchetError(DoubleRatchetError.Type.ConversationNotFound)
        if (!conversation.isReadyForMessageSending()) throw DoubleRatchetError(DoubleRatchetError.Type.ConversationNotSetup)
        val isNewSequence = conversation.lastMessageReceivedType != Conversation.MessageType.Sent

        return if (isNewSequence) {
            sendNewSequenceData(conversation)
        } else {
            sendOldSequence(conversation)
        }
    }

    private suspend fun sendNewSequenceData(conversation: Conversation): SendMessageData {
        val newKeyPair = doubleRatchetKeyRepository.generateKeyPair()
        val sharedSecret = doubleRatchetKeyRepository.createDiffieHellmanSharedSecret(
            conversation.contactPublicKey!!,
            newKeyPair.privateKey,
        )
        val derivedKeyPair = doubleRatchetKeyRepository.deriveKeys(conversation.sendChainKey!!, sharedSecret)
        sharedSecret.destroy()

        conversation.apply {
            personalKeyPair = newKeyPair
            sentLastMessageData = MessageConversationCounter(
                message = conversation.sentLastMessageData?.message?.inc() ?: 0u,
                sequence = 0u,
            )
            lastMessageReceivedType = Conversation.MessageType.Sent
            sendChainKey = derivedKeyPair.chainKey
        }

        doubleRatchetLocalDatasource.saveOrUpdateConversation(conversation)
        derivedKeyPair.chainKey.destroy()
        newKeyPair.privateKey.destroy()

        val chainKeyToSend = doubleRatchetLocalDatasource.retrieveChainKey(conversation.id)
        return SendMessageData(
            messageHeader = MessageHeader(
                counter = conversation.sentLastMessageData!!,
                publicKey = conversation.personalKeyPair.publicKey,
                chainKey = chainKeyToSend,
            ),
            messageKey = derivedKeyPair.messageKey,
        )
    }

    private suspend fun sendOldSequence(conversation: Conversation): SendMessageData {
        val derivedKeyPair = doubleRatchetKeyRepository.deriveKey(conversation.sendChainKey!!)

        val chainKeyToSend = doubleRatchetLocalDatasource.retrieveChainKey(conversation.id)
        val messageData = SendMessageData(
            messageHeader = MessageHeader(
                counter = MessageConversationCounter(
                    message = conversation.sentLastMessageData?.message?.inc() ?: 0u,
                    sequence = conversation.sentLastMessageData?.sequence?.inc() ?: 0u,
                ),
                publicKey = conversation.personalKeyPair.publicKey,
                chainKey = chainKeyToSend,
            ),
            messageKey = derivedKeyPair.messageKey,
        )

        conversation.apply {
            sendChainKey = derivedKeyPair.chainKey
            sentLastMessageData = MessageConversationCounter(
                message = conversation.sentLastMessageData?.message?.inc() ?: 0u,
                sequence = conversation.sentLastMessageData?.sequence?.inc() ?: 0u,
            )
        }
        doubleRatchetLocalDatasource.saveOrUpdateConversation(conversation)
        derivedKeyPair.chainKey.destroy()

        return messageData
    }

    /**
     * @return the key needed to decrypt the message attached to the messageHeader
     */
    suspend fun getReceiveKey(messageHeader: MessageHeader, conversationId: DoubleRatchetUUID): DRMessageKey {
        val conversation = doubleRatchetLocalDatasource.getConversation(conversationId)
            ?: throw DoubleRatchetError(DoubleRatchetError.Type.ConversationNotFound)

        // If the chains keys are unknown, we need to set up conversation with the receiveChainKey in the messageHeader.
        if (!conversation.isReadyForMessageReceiving()) {
            setupConversationOnReceiveMessage(messageHeader, conversation)
        }

        val isMessageKeyAlreadyGenerated = conversation.receivedLastMessageNumber?.let { messageCount ->
            messageCount >= messageHeader.counter.message
        } ?: false

        return if (isMessageKeyAlreadyGenerated) {
            popStoredMessageKey(messageHeader, conversationId)
        } else {
            computeNextMessageKey(messageHeader, conversation)
        }
    }

    private suspend fun popStoredMessageKey(
        messageHeader: MessageHeader,
        conversationId: DoubleRatchetUUID,
    ): DRMessageKey {
        val messageKeyId = getMessageKeyId(conversationId, messageHeader.counter.message)
        return doubleRatchetLocalDatasource.popMessageKey(messageKeyId)
            ?: throw DoubleRatchetError(DoubleRatchetError.Type.MessageKeyNotFound)
    }

    private suspend fun computeNextMessageKey(
        messageHeader: MessageHeader,
        conversation: Conversation,
    ): DRMessageKey {
        var workingConversation = conversation
        val lastMessageNumber = workingConversation.receivedLastMessageNumber

        val newSequenceMessageNumber: UInt? = if (messageHeader.publicKey.contentEquals(workingConversation.contactPublicKey)) {
            null
        } else {
            messageHeader.counter.message - messageHeader.counter.sequence
        }

        var messageNumber = lastMessageNumber?.inc() ?: 0u
        var messageKey: DRMessageKey? = null
        val sharedSecret = lazy { DRSharedSecret.empty() }
        val derivedKeyPair = DerivedKeyPair.empty()
        while (messageKey == null) {
            if (messageNumber == newSequenceMessageNumber) {
                sharedSecret.value.use {
                    receiveNewSequenceMessage(messageHeader.publicKey, workingConversation, it, derivedKeyPair)
                }
            } else {
                receiveOldSequenceMessage(workingConversation, derivedKeyPair)
            }
            workingConversation = doubleRatchetLocalDatasource.getConversation(conversation.id)
                ?: throw DoubleRatchetError(DoubleRatchetError.Type.ConversationNotFound)

            if (messageNumber == messageHeader.counter.message) {
                messageKey = derivedKeyPair.messageKey
            } else {
                doubleRatchetLocalDatasource.saveMessageKey(
                    id = getMessageKeyId(conversation.id, messageNumber),
                    key = derivedKeyPair.messageKey,
                )
                derivedKeyPair.messageKey.destroy()
                messageNumber++
            }
        }

        // Once the contact has replied, we can delete the chain from database
        doubleRatchetLocalDatasource.deleteChainKey(id = conversation.id)
        return messageKey
    }

    private suspend fun receiveOldSequenceMessage(
        conversation: Conversation,
        derivedKeyPair: DerivedKeyPair,
    ) {
        doubleRatchetKeyRepository.deriveKey(conversation.receiveChainKey!!, derivedKeyPair)
        conversation.apply {
            receiveChainKey = derivedKeyPair.chainKey
            receivedLastMessageNumber = conversation.receivedLastMessageNumber?.inc() ?: 1u
        }
        doubleRatchetLocalDatasource.saveOrUpdateConversation(conversation)
        derivedKeyPair.chainKey.destroy()
    }

    private suspend fun receiveNewSequenceMessage(
        publicKey: DRPublicKey,
        conversation: Conversation,
        sharedSecret: DRSharedSecret,
        derivedKeyPair: DerivedKeyPair,
    ) {
        doubleRatchetKeyRepository.createDiffieHellmanSharedSecret(publicKey, conversation.personalKeyPair.privateKey, sharedSecret)
        doubleRatchetKeyRepository.deriveKeys(conversation.receiveChainKey!!, sharedSecret, derivedKeyPair)
        sharedSecret.destroy()
        conversation.apply {
            receiveChainKey = derivedKeyPair.chainKey
            contactPublicKey = publicKey
            receivedLastMessageNumber = conversation.receivedLastMessageNumber?.inc() ?: 0u
            lastMessageReceivedType = Conversation.MessageType.Received
        }
        doubleRatchetLocalDatasource.saveOrUpdateConversation(conversation)
        derivedKeyPair.chainKey.destroy()
    }

    private suspend fun setupConversationOnReceiveMessage(
        messageHeader: MessageHeader,
        conversation: Conversation,
    ) {
        val receiveChainKey: DRChainKey =
            messageHeader.chainKey ?: throw DoubleRatchetError(DoubleRatchetError.Type.RequiredChainKeyMissing)
        val sendChainKey: DRChainKey = doubleRatchetKeyRepository.deriveKey(receiveChainKey).chainKey
        conversation.apply {
            this.sendChainKey = sendChainKey
            this.receiveChainKey = receiveChainKey
        }
    }

    private fun getMessageKeyId(conversationId: DoubleRatchetUUID, messageNumber: UInt): String {
        return "${conversationId.uuidString()} - $messageNumber"
    }
}
