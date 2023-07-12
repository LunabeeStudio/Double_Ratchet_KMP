package studio.lunabee.doubleratchet

import studio.lunabee.doubleratchet.crypto.DoubleRatchetCryptoRepository
import studio.lunabee.doubleratchet.model.AsymmetricKeyPair
import studio.lunabee.doubleratchet.model.Conversation
import studio.lunabee.doubleratchet.model.DoubleRatchetError
import studio.lunabee.doubleratchet.model.DoubleRatchetUUID
import studio.lunabee.doubleratchet.model.InvitationData
import studio.lunabee.doubleratchet.model.LastMessageConversationData
import studio.lunabee.doubleratchet.model.MessageHeader
import studio.lunabee.doubleratchet.model.SendMessageData
import studio.lunabee.doubleratchet.model.createRandomUUID
import studio.lunabee.doubleratchet.storage.DoubleRatchetLocalDatasource

class DoubleRatchetEngine(
    private val doubleRatchetLocalDatasource: DoubleRatchetLocalDatasource,
    private val doubleRatchetCryptoRepository: DoubleRatchetCryptoRepository,
) {

    suspend fun createInvitation(): InvitationData {
        val newConversationId: DoubleRatchetUUID = createRandomUUID()
        val keyPair: AsymmetricKeyPair = doubleRatchetCryptoRepository.generateKeyPair()
        val conversation = Conversation(
            id = newConversationId,
            personalPublicKey = keyPair.publicKey,
            personalPrivateKey = keyPair.privateKey,
        )
        doubleRatchetLocalDatasource.saveOrUpdateConversation(conversation)
        return InvitationData(
            conversationId = newConversationId,
            publicKey = keyPair.publicKey
        )
    }

    /**
     * Generate the data needed to start a conversation with a new person which sent you an invitation
     * @return the id assign to the contact created
     */
    suspend fun createNewConversationFromInvitation(contactPublicKey: ByteArray): DoubleRatchetUUID {
        val newConversationId: DoubleRatchetUUID = createRandomUUID()
        val keyPair: AsymmetricKeyPair = doubleRatchetCryptoRepository.generateKeyPair()
        val sendChainKey: ByteArray = doubleRatchetCryptoRepository.generateChainKey()
        doubleRatchetLocalDatasource.saveMessageKey(id = newConversationId.uuidString(), sendChainKey)
        val receiveChainKey: ByteArray = doubleRatchetCryptoRepository.deriveKey(sendChainKey).nextChainKey
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
        val newKeyPair = doubleRatchetCryptoRepository.generateKeyPair()
        val sharedSecret = doubleRatchetCryptoRepository.createDiffieHellmanSharedSecret(
            conversation.contactPublicKey!!,
            newKeyPair.privateKey
        )
        val derivedKeyPair = doubleRatchetCryptoRepository.deriveKeys(conversation.sendChainKey!!, sharedSecret)
        val newConversation = conversation.copy(
            personalPublicKey = newKeyPair.publicKey,
            personalPrivateKey = newKeyPair.privateKey,
            sentLastMessageData = LastMessageConversationData(
                messageNumber = (conversation.sentLastMessageData?.messageNumber ?: -1) + 1,
                sequenceNumber = 0
            ),
            lastMessageReceivedType = Conversation.MessageType.Sent,
            sendChainKey = derivedKeyPair.nextChainKey
        )
        doubleRatchetLocalDatasource.saveOrUpdateConversation(newConversation)
        val chainKeyToSend = doubleRatchetLocalDatasource.retrieveMessageKey(conversation.id.uuidString())
        return SendMessageData(
            messageHeader = MessageHeader(
                messageNumber = (conversation.sentLastMessageData?.messageNumber ?: -1) + 1,
                sequenceMessageNumber = 0,
                publicKey = newKeyPair.publicKey,
                chainKey = chainKeyToSend
            ),
            messageKey = derivedKeyPair.messageKey,
        )
    }

    private suspend fun sendOldSequence(conversation: Conversation): SendMessageData {
        val derivedKeyPair = doubleRatchetCryptoRepository.deriveKey(conversation.sendChainKey!!)
        val newConversation = conversation.copy(
            sendChainKey = derivedKeyPair.nextChainKey,
            sentLastMessageData = LastMessageConversationData(
                messageNumber = (conversation.sentLastMessageData?.messageNumber ?: -1) + 1,
                sequenceNumber = (conversation.sentLastMessageData?.sequenceNumber ?: -1) + 1
            ),
        )
        doubleRatchetLocalDatasource.saveOrUpdateConversation(newConversation)
        val chainKeyToSend = doubleRatchetLocalDatasource.retrieveMessageKey(conversation.id.uuidString())
        return SendMessageData(
            messageHeader = MessageHeader(
                messageNumber = (conversation.sentLastMessageData?.messageNumber ?: -1) + 1,
                sequenceMessageNumber = (conversation.sentLastMessageData?.sequenceNumber ?: -1) + 1,
                publicKey = conversation.personalPublicKey,
                chainKey = chainKeyToSend
            ),
            messageKey = derivedKeyPair.messageKey
        )
    }

    /**
     * Return the key needed to decrypt the message attached to the messageHeader
     */
    suspend fun getReceiveKey(messageHeader: MessageHeader, conversationId: DoubleRatchetUUID): ByteArray {
        var conversation = doubleRatchetLocalDatasource.getConversation(conversationId)
            ?: throw DoubleRatchetError(DoubleRatchetError.Type.ConversationNotFound)
        // If the chains keys are unknown, we need to setup conversation with the receiveChainKey in the messageHeader.
        if (!conversation.isReadyForMessageReceiving()) {
            conversation = setupConversationOnReceiveMessage(messageHeader, conversation)
        }

        // If the messageKey has already been generated
        if ((conversation.receivedLastMessageData?.messageNumber ?: -1) >= messageHeader.messageNumber) {
            return doubleRatchetLocalDatasource.retrieveMessageKey(id = getMessageKeyId(conversationId, messageHeader.messageNumber))
                ?.also {
                    doubleRatchetLocalDatasource.deleteMessageKey(getMessageKeyId(conversationId, messageHeader.messageNumber))
                } ?: throw DoubleRatchetError(DoubleRatchetError.Type.MessageKeyNotFound)
        }

        val lastMessageNumber = conversation.receivedLastMessageData?.messageNumber ?: -1

        val newSequenceMessageNumber: Int? = if (messageHeader.publicKey.contentEquals(conversation.contactPublicKey)) {
            null
        } else {
            messageHeader.messageNumber - messageHeader.sequenceMessageNumber
        }

        var messageNumber: Int = lastMessageNumber + 1
        var messageKey: ByteArray? = null
        while (messageKey == null) {
            val currentMessageKey = if (messageNumber == newSequenceMessageNumber) {
                receiveNewSequenceMessage(messageHeader.publicKey, conversation)
            } else {
                receiveOldSequenceMessage(conversation)
            }
            conversation = doubleRatchetLocalDatasource.getConversation(conversationId)
                ?: throw DoubleRatchetError(DoubleRatchetError.Type.ConversationNotFound)
            if (messageNumber == messageHeader.messageNumber) {
                messageKey = currentMessageKey
            } else {
                doubleRatchetLocalDatasource.saveMessageKey(id = getMessageKeyId(conversationId, messageNumber), key = currentMessageKey)
            }
            messageNumber++
        }
        // Once the contact has replied, we can delete the chain from database
        doubleRatchetLocalDatasource.deleteMessageKey(id = conversationId.uuidString())
        return messageKey
    }

    private suspend fun receiveOldSequenceMessage(conversation: Conversation): ByteArray {
        val derivedKeyPair = doubleRatchetCryptoRepository.deriveKey(conversation.receiveChainKey!!)
        val newConversation = conversation.copy(
            receiveChainKey = derivedKeyPair.nextChainKey,
            receivedLastMessageData = LastMessageConversationData(
                messageNumber = (conversation.receivedLastMessageData?.messageNumber ?: 0) + 1,
                sequenceNumber = (conversation.receivedLastMessageData?.sequenceNumber ?: 0) + 1,
            ),
        )
        doubleRatchetLocalDatasource.saveOrUpdateConversation(newConversation)
        return derivedKeyPair.messageKey
    }

    private suspend fun receiveNewSequenceMessage(publicKey: ByteArray, conversation: Conversation): ByteArray {
        val sharedSecret = doubleRatchetCryptoRepository.createDiffieHellmanSharedSecret(publicKey, conversation.personalPrivateKey)
        val derivedKeyPair = doubleRatchetCryptoRepository.deriveKeys(conversation.receiveChainKey!!, sharedSecret)
        val newConversation = conversation.copy(
            receiveChainKey = derivedKeyPair.nextChainKey,
            contactPublicKey = publicKey,
            receivedLastMessageData = LastMessageConversationData(
                messageNumber = (conversation.receivedLastMessageData?.messageNumber ?: -1) + 1,
                sequenceNumber = 0
            ),
            lastMessageReceivedType = Conversation.MessageType.Received,
        )
        doubleRatchetLocalDatasource.saveOrUpdateConversation(newConversation)
        return derivedKeyPair.messageKey
    }

    private suspend fun setupConversationOnReceiveMessage(messageHeader: MessageHeader, conversation: Conversation): Conversation {
        val receiveChainKey: ByteArray = messageHeader.chainKey ?: throw DoubleRatchetError(DoubleRatchetError.Type.RequiredChainKeyMissing)
        val sendChainKey: ByteArray = doubleRatchetCryptoRepository.deriveKey(receiveChainKey).nextChainKey
        return conversation.copy(sendChainKey = sendChainKey, receiveChainKey = receiveChainKey)
    }

    private fun getMessageKeyId(conversationId: DoubleRatchetUUID, messageNumber: Int): String {
        return "${conversationId.uuidString()} - $messageNumber"
    }
}