package studio.lunabee.doubleratchet

import studio.lunabee.doubleratchet.crypto.CryptoRepository
import studio.lunabee.doubleratchet.model.AsymmetricKeyPair
import studio.lunabee.doubleratchet.model.Conversation
import studio.lunabee.doubleratchet.model.InvitationData
import studio.lunabee.doubleratchet.model.LastMessageConversationData
import studio.lunabee.doubleratchet.model.MessageHeader
import studio.lunabee.doubleratchet.model.SendMessageData
import studio.lunabee.doubleratchet.storage.LocalDatasource
import studio.lunabee.doubleratchet.storage.TestLocalDatasourceImpl
import kotlin.random.Random

class DoubleRatchetEngine(
    private val localDatasource: LocalDatasource = TestLocalDatasourceImpl(),
    private val cryptoRepository: CryptoRepository,
) {

    fun createInvitation(): InvitationData {
        val newConversationId = Random.nextInt().toString()
        val keyPair: AsymmetricKeyPair = cryptoRepository.generateKeyPair()
        val conversation = Conversation(
            id = newConversationId,
            personalPrivateKey = keyPair.privateKey,
            personalPublicKey = keyPair.publicKey,
        )
        localDatasource.saveOrUpdateConversation(conversation)
        return InvitationData(
            conversationId = newConversationId,
            publicKey = keyPair.publicKey
        )
    }

    /**
     * Generate the data needed to start a conversation with a new person which sent you an invitation
     * @return the id assign to the contact created
     */
    fun createNewConversationFromInvitation(contactPublicKey: ByteArray): String {
        val newConversationId = Random.nextInt().toString()
        val keyPair: AsymmetricKeyPair = cryptoRepository.generateKeyPair()
        val sendChainKey: ByteArray = cryptoRepository.generateChainKey()
        localDatasource.saveMessageKey(id = newConversationId, sendChainKey)
        val receiveChainKey: ByteArray = cryptoRepository.deriveKey(sendChainKey).nextChainKey
        val conversation = Conversation(
            id = newConversationId,
            sendChainKey = sendChainKey,
            receiveChainKey = receiveChainKey,
            contactPublicKey = contactPublicKey,
            personalPrivateKey = keyPair.privateKey,
            personalPublicKey = keyPair.publicKey,
        )
        localDatasource.saveOrUpdateConversation(conversation)
        return newConversationId
    }

    /**
     * Generate new message header to attach to the message you want to send, and the message key to use to encrypt the message
     */
    fun getSendData(conversationId: String): SendMessageData {
        val conversation = localDatasource.getConversation(conversationId) ?: error("the conversation doesn't exist")
        if (!conversation.isReadyForMessageSending()) error("The conversation setup is not done")
        val isNewSequence = conversation.lastMessageReceivedType != Conversation.MessageType.Sent
        return if (isNewSequence) sendNewSequenceData(conversation) else sendOldSequence(conversation)
    }

    private fun sendNewSequenceData(conversation: Conversation): SendMessageData {
        val newKeyPair = cryptoRepository.generateKeyPair()
        val sharedSecret = cryptoRepository.createDiffieHellmanSharedSecret(conversation.contactPublicKey!!, newKeyPair.privateKey)
        val derivedKeyPair = cryptoRepository.deriveKeys(conversation.sendChainKey!!, sharedSecret)
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
        localDatasource.saveOrUpdateConversation(newConversation)
        val chainKeyToSend = localDatasource.retrieveMessageKey(conversation.id)
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

    private fun sendOldSequence(conversation: Conversation): SendMessageData {
        val derivedKeyPair = cryptoRepository.deriveKey(conversation.sendChainKey!!)
        val newConversation = conversation.copy(
            sendChainKey = derivedKeyPair.nextChainKey,
            sentLastMessageData = LastMessageConversationData(
                messageNumber = (conversation.sentLastMessageData?.messageNumber ?: -1) + 1,
                sequenceNumber = (conversation.sentLastMessageData?.sequenceNumber ?: -1) + 1
            ),
        )
        localDatasource.saveOrUpdateConversation(newConversation)
        val chainKeyToSend = localDatasource.retrieveMessageKey(conversation.id)
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
    fun getReceiveKey(messageHeader: MessageHeader, conversationId: String): ByteArray {
        var conversation = localDatasource.getConversation(conversationId) ?: error("the conversation doesn't exist")
        // If the chains keys are unknown, we need to setup conversation with the receiveChainKey in the messageHeader.
        if (!conversation.isReadyForMessageReceiving()) {
            conversation = setupConversationOnReceiveMessage(messageHeader, conversation)
        }

        // If the messageKey has already been generated
        if ((conversation.receivedLastMessageData?.messageNumber ?: 0) > messageHeader.messageNumber) {
            return localDatasource.retrieveMessageKey(id = getMessageKeyId(conversationId, messageHeader.messageNumber))?.also {
                localDatasource.deleteMessageKey(getMessageKeyId(conversationId, messageHeader.messageNumber))
            } ?: error("the message key is not found")
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
            conversation = localDatasource.getConversation(conversationId) ?: error("the conversation doesn't exist")
            if (messageNumber == messageHeader.messageNumber) {
                messageKey = currentMessageKey
            } else {
                localDatasource.saveMessageKey(id = getMessageKeyId(conversationId, messageNumber), key = currentMessageKey)
            }
            messageNumber++
        }
        // Once the contact responded we can delete the chain from database
        localDatasource.deleteMessageKey(id = conversationId)
        return messageKey
    }

    private fun receiveOldSequenceMessage(conversation: Conversation): ByteArray {
        val derivedKeyPair = cryptoRepository.deriveKey(conversation.receiveChainKey!!)
        val newConversation = conversation.copy(
            receiveChainKey = derivedKeyPair.nextChainKey,
            receivedLastMessageData = LastMessageConversationData(
                messageNumber = (conversation.receivedLastMessageData?.messageNumber ?: 0) + 1,
                sequenceNumber = (conversation.receivedLastMessageData?.sequenceNumber ?: 0) + 1,
            ),
        )
        localDatasource.saveOrUpdateConversation(newConversation)
        return derivedKeyPair.messageKey
    }

    private fun receiveNewSequenceMessage(publicKey: ByteArray, conversation: Conversation): ByteArray {
        val sharedSecret = cryptoRepository.createDiffieHellmanSharedSecret(publicKey, conversation.personalPrivateKey)
        val derivedKeyPair = cryptoRepository.deriveKeys(conversation.receiveChainKey!!, sharedSecret)
        val newConversation = conversation.copy(
            receiveChainKey = derivedKeyPair.nextChainKey,
            contactPublicKey = publicKey,
            receivedLastMessageData = LastMessageConversationData(
                messageNumber = (conversation.receivedLastMessageData?.messageNumber ?: -1) + 1,
                sequenceNumber = 0
            ),
            lastMessageReceivedType = Conversation.MessageType.Received,
        )
        localDatasource.saveOrUpdateConversation(newConversation)
        return derivedKeyPair.messageKey
    }

    private fun setupConversationOnReceiveMessage(messageHeader: MessageHeader, conversation: Conversation): Conversation {
        val receiveChainKey: ByteArray = messageHeader.chainKey ?: error("can't setup conversation")
        val sendChainKey: ByteArray = cryptoRepository.deriveKey(receiveChainKey).nextChainKey
        return conversation.copy(sendChainKey = sendChainKey, receiveChainKey = receiveChainKey)
    }

    private fun getMessageKeyId(conversationId: String, messageNumber: Int): String {
        return "$conversationId - $messageNumber"
    }
}