package studio.lunabee.doubleratchet.model

class Conversation private constructor(
    val id: DoubleRatchetUUID,
    personalKeyPair: AsymmetricKeyPair,
    sendChainKey: ChainKey? = null,
    receiveChainKey: ChainKey? = null,
    contactPublicKey: PublicKey? = null,
    lastMessageReceivedType: MessageType? = null,
    sentLastMessageData: MessageConversationCounter? = null,
    receivedLastMessageData: MessageConversationCounter? = null,
) {
    var personalKeyPair: AsymmetricKeyPair = personalKeyPair
        internal set
    var sendChainKey: ChainKey? = sendChainKey
        internal set
    var receiveChainKey: ChainKey? = receiveChainKey
        internal set
    var contactPublicKey: PublicKey? = contactPublicKey
        internal set
    var lastMessageReceivedType: MessageType? = lastMessageReceivedType
        internal set
    var sentLastMessageData: MessageConversationCounter? = sentLastMessageData
        internal set
    var receivedLastMessageData: MessageConversationCounter? = receivedLastMessageData
        internal set

    fun isReadyForMessageSending(): Boolean {
        return sendChainKey != null && contactPublicKey != null
    }

    fun isReadyForMessageReceiving(): Boolean {
        return receiveChainKey != null && contactPublicKey != null
    }

    enum class MessageType {
        Sent, Received
    }

    companion object {
        fun createNew(id: DoubleRatchetUUID, personalKeyPair: AsymmetricKeyPair): Conversation = Conversation(
            id = id,
            personalKeyPair = personalKeyPair,
        )

        fun createFromInvitation(
            id: DoubleRatchetUUID,
            personalKeyPair: AsymmetricKeyPair,
            sendChainKey: ChainKey,
            receiveChainKey: ChainKey,
            contactPublicKey: PublicKey,
        ): Conversation = Conversation(
            id = id,
            personalKeyPair = personalKeyPair,
            sendChainKey = sendChainKey,
            receiveChainKey = receiveChainKey,
            contactPublicKey = contactPublicKey,
        )
    }
}
