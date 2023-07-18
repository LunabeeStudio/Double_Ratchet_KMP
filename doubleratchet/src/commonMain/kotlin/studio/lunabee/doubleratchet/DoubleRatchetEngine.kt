package studio.lunabee.doubleratchet

import studio.lunabee.doubleratchet.crypto.DoubleRatchetKeyRepository
import studio.lunabee.doubleratchet.model.AsymmetricKeyPair
import studio.lunabee.doubleratchet.model.ChainKey
import studio.lunabee.doubleratchet.model.Conversation
import studio.lunabee.doubleratchet.model.DoubleRatchetError
import studio.lunabee.doubleratchet.model.DoubleRatchetUUID
import studio.lunabee.doubleratchet.model.InvitationData
import studio.lunabee.doubleratchet.model.MessageConversationCounter
import studio.lunabee.doubleratchet.model.MessageHeader
import studio.lunabee.doubleratchet.model.MessageKey
import studio.lunabee.doubleratchet.model.SendMessageData
import studio.lunabee.doubleratchet.model.createRandomUUID
import studio.lunabee.doubleratchet.storage.DoubleRatchetLocalDatasource

class DoubleRatchetEngine(
    private val doubleRatchetLocalDatasource: DoubleRatchetLocalDatasource,
    private val doubleRatchetKeyRepository: DoubleRatchetKeyRepository,
) {

    suspend fun createInvitation(newConversationId: DoubleRatchetUUID = createRandomUUID()): InvitationData {
        val keyPair: AsymmetricKeyPair = doubleRatchetKeyRepository.generateKeyPair()
        val conversation = Conversation(
            id = newConversationId,
            personalPublicKey = keyPair.publicKey,
            personalPrivateKey = keyPair.privateKey,
        )
        doubleRatchetLocalDatasource.saveOrUpdateConversation(conversation)
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
        contactPublicKey: ByteArray,
        newConversationId: DoubleRatchetUUID = createRandomUUID(),
    ): DoubleRatchetUUID {
        val keyPair: AsymmetricKeyPair = doubleRatchetKeyRepository.generateKeyPair()
        val sendChainKey = doubleRatchetKeyRepository.generateChainKey()
        doubleRatchetLocalDatasource.saveChainKey(id = newConversationId.uuidString(), sendChainKey)
        val receiveChainKey: ChainKey = doubleRatchetKeyRepository.deriveKey(sendChainKey).nextChainKey
        val conversation = Conversation(
            id = newConversationId,
            personalPublicKey = keyPair.publicKey,
            personalPrivateKey = keyPair.privateKey,
            sendChainKey = sendChainKey,
            receiveChainKey = receiveChainKey,
            contactPublicKey = contactPublicKey,
        )
        doubleRatchetLocalDatasource.saveOrUpdateConversation(conversation)
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
        return if (isNewSequence) sendNewSequenceData(conversation) else sendOldSequence(conversation)
    }

    private suspend fun sendNewSequenceData(conversation: Conversation): SendMessageData {
        val newKeyPair = doubleRatchetKeyRepository.generateKeyPair()
        val sharedSecret = doubleRatchetKeyRepository.createDiffieHellmanSharedSecret(
            conversation.contactPublicKey!!,
            newKeyPair.privateKey,
        )
        val derivedKeyPair = doubleRatchetKeyRepository.deriveKeys(conversation.sendChainKey!!, sharedSecret)
        val newConversation = conversation.copy(
            personalPublicKey = newKeyPair.publicKey,
            personalPrivateKey = newKeyPair.privateKey,
            sentLastMessageData = MessageConversationCounter(
                message = conversation.sentLastMessageData?.message?.inc() ?: 0u,
                sequence = 0u,
            ),
            lastMessageReceivedType = Conversation.MessageType.Sent,
            sendChainKey = derivedKeyPair.nextChainKey,
        )
        doubleRatchetLocalDatasource.saveOrUpdateConversation(newConversation)
        val chainKeyToSend = doubleRatchetLocalDatasource.retrieveChainKey(conversation.id.uuidString())
        return SendMessageData(
            messageHeader = MessageHeader(
                counter = MessageConversationCounter(
                    message = conversation.sentLastMessageData?.message?.inc() ?: 0u,
                    sequence = 0u,
                ),
                publicKey = newKeyPair.publicKey,
                chainKey = chainKeyToSend,
            ),
            messageKey = derivedKeyPair.messageKey,
        )
    }

    private suspend fun sendOldSequence(conversation: Conversation): SendMessageData {
        val derivedKeyPair = doubleRatchetKeyRepository.deriveKey(conversation.sendChainKey!!)
        val newConversation = conversation.copy(
            sendChainKey = derivedKeyPair.nextChainKey,
            sentLastMessageData = MessageConversationCounter(
                message = conversation.sentLastMessageData?.message?.inc() ?: 0u,
                sequence = conversation.sentLastMessageData?.sequence?.inc() ?: 0u,
            ),
        )
        doubleRatchetLocalDatasource.saveOrUpdateConversation(newConversation)
        val chainKeyToSend = doubleRatchetLocalDatasource.retrieveChainKey(conversation.id.uuidString())
        return SendMessageData(
            messageHeader = MessageHeader(
                counter = MessageConversationCounter(
                    message = conversation.sentLastMessageData?.message?.inc() ?: 0u,
                    sequence = conversation.sentLastMessageData?.sequence?.inc() ?: 0u,
                ),
                publicKey = conversation.personalPublicKey,
                chainKey = chainKeyToSend,
            ),
            messageKey = derivedKeyPair.messageKey,
        )
    }

    /**
     * @return the key needed to decrypt the message attached to the messageHeader
     */
    suspend fun getReceiveKey(messageHeader: MessageHeader, conversationId: DoubleRatchetUUID): MessageKey {
        var conversation = doubleRatchetLocalDatasource.getConversation(conversationId)
            ?: throw DoubleRatchetError(DoubleRatchetError.Type.ConversationNotFound)
        // If the chains keys are unknown, we need to set up conversation with the receiveChainKey in the messageHeader.
        if (!conversation.isReadyForMessageReceiving()) {
            conversation = setupConversationOnReceiveMessage(messageHeader, conversation)
        }

        val isMessageKeyAlreadyGenerated = conversation.receivedLastMessageData?.message?.let { messageCount ->
            messageCount >= messageHeader.counter.message
        } ?: false
        if (isMessageKeyAlreadyGenerated) {
            val retrieveMessageKey = doubleRatchetLocalDatasource.retrieveMessageKey(
                id = getMessageKeyId(
                    conversationId,
                    messageHeader.counter.message,
                ),
            ) ?: throw DoubleRatchetError(DoubleRatchetError.Type.MessageKeyNotFound)
            doubleRatchetLocalDatasource.deleteMessageKey(
                getMessageKeyId(
                    conversationId,
                    messageHeader.counter.message,
                ),
            )
            return retrieveMessageKey
        }

        val lastMessageNumber = conversation.receivedLastMessageData?.message

        val newSequenceMessageNumber: UInt? = if (messageHeader.publicKey.contentEquals(conversation.contactPublicKey)) {
            null
        } else {
            messageHeader.counter.message - messageHeader.counter.sequence
        }

        var messageNumber = lastMessageNumber?.inc() ?: 0u
        var messageKey: MessageKey? = null
        while (messageKey == null) {
            val currentMessageKey = if (messageNumber == newSequenceMessageNumber) {
                receiveNewSequenceMessage(messageHeader.publicKey, conversation)
            } else {
                receiveOldSequenceMessage(conversation)
            }
            conversation = doubleRatchetLocalDatasource.getConversation(conversationId)
                ?: throw DoubleRatchetError(DoubleRatchetError.Type.ConversationNotFound)
            if (messageNumber == messageHeader.counter.message) {
                messageKey = currentMessageKey
            } else {
                doubleRatchetLocalDatasource.saveMessageKey(
                    id = getMessageKeyId(conversationId, messageNumber),
                    key = currentMessageKey,
                )
            }
            messageNumber++
        }
        // Once the contact has replied, we can delete the chain from database
        doubleRatchetLocalDatasource.deleteMessageKey(id = conversationId.uuidString())
        return messageKey
    }

    private suspend fun receiveOldSequenceMessage(conversation: Conversation): MessageKey {
        val derivedKeyPair = doubleRatchetKeyRepository.deriveKey(conversation.receiveChainKey!!)
        val newConversation = conversation.copy(
            receiveChainKey = derivedKeyPair.nextChainKey,
            receivedLastMessageData = MessageConversationCounter(
                message = conversation.receivedLastMessageData?.message?.inc() ?: 1u,
                sequence = conversation.receivedLastMessageData?.sequence?.inc() ?: 1u,
            ),
        )
        doubleRatchetLocalDatasource.saveOrUpdateConversation(newConversation)
        return derivedKeyPair.messageKey
    }

    private suspend fun receiveNewSequenceMessage(publicKey: ByteArray, conversation: Conversation): MessageKey {
        val sharedSecret =
            doubleRatchetKeyRepository.createDiffieHellmanSharedSecret(publicKey, conversation.personalPrivateKey)
        val derivedKeyPair = doubleRatchetKeyRepository.deriveKeys(conversation.receiveChainKey!!, sharedSecret)
        val newConversation = conversation.copy(
            receiveChainKey = derivedKeyPair.nextChainKey,
            contactPublicKey = publicKey,
            receivedLastMessageData = MessageConversationCounter(
                message = conversation.receivedLastMessageData?.message?.inc() ?: 0u,
                sequence = 0u,
            ),
            lastMessageReceivedType = Conversation.MessageType.Received,
        )
        doubleRatchetLocalDatasource.saveOrUpdateConversation(newConversation)
        return derivedKeyPair.messageKey
    }

    private suspend fun setupConversationOnReceiveMessage(
        messageHeader: MessageHeader,
        conversation: Conversation,
    ): Conversation {
        val receiveChainKey: ChainKey =
            messageHeader.chainKey ?: throw DoubleRatchetError(DoubleRatchetError.Type.RequiredChainKeyMissing)
        val sendChainKey: ChainKey = doubleRatchetKeyRepository.deriveKey(receiveChainKey).nextChainKey
        return conversation.copy(sendChainKey = sendChainKey, receiveChainKey = receiveChainKey)
    }

    private fun getMessageKeyId(conversationId: DoubleRatchetUUID, messageNumber: UInt): String {
        return "${conversationId.uuidString()} - $messageNumber"
    }
}
