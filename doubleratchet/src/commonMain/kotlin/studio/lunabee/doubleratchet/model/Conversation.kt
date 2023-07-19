package studio.lunabee.doubleratchet.model

class Conversation private constructor(
    val id: DoubleRatchetUUID,
    personalKeyPair: AsymmetricKeyPair,
    sendChainKey: DRChainKey? = null,
    receiveChainKey: DRChainKey? = null,
    contactPublicKey: DRPublicKey? = null,
    lastMessageReceivedType: MessageType? = null,
    sentLastMessageData: MessageConversationCounter? = null,
    receivedLastMessageNumber: UInt? = null,
) {
    var personalKeyPair: AsymmetricKeyPair = personalKeyPair
        internal set
    var sendChainKey: DRChainKey? = sendChainKey
        internal set
    var receiveChainKey: DRChainKey? = receiveChainKey
        internal set
    var contactPublicKey: DRPublicKey? = contactPublicKey
        internal set
    var lastMessageReceivedType: MessageType? = lastMessageReceivedType
        internal set
    var sentLastMessageData: MessageConversationCounter? = sentLastMessageData
        internal set
    var receivedLastMessageNumber: UInt? = receivedLastMessageNumber
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
            sendChainKey: DRChainKey,
            receiveChainKey: DRChainKey,
            contactPublicKey: DRPublicKey,
        ): Conversation = Conversation(
            id = id,
            personalKeyPair = personalKeyPair,
            sendChainKey = sendChainKey,
            receiveChainKey = receiveChainKey,
            contactPublicKey = contactPublicKey,
        )
    }
}
