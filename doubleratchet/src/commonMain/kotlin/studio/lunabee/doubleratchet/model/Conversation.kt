package studio.lunabee.doubleratchet.model

data class Conversation(
    val id: DoubleRatchetUUID,
    val personalKeyPair: AsymmetricKeyPair,
    val sendChainKey: ChainKey? = null,
    val receiveChainKey: ChainKey? = null,
    val contactPublicKey: PublicKey? = null,
    val lastMessageReceivedType: MessageType? = null,
    val sentLastMessageData: MessageConversationCounter? = null,
    val receivedLastMessageData: MessageConversationCounter? = null,
) {
    fun isReadyForMessageSending(): Boolean {
        return sendChainKey != null && contactPublicKey != null
    }

    fun isReadyForMessageReceiving(): Boolean {
        return receiveChainKey != null && contactPublicKey != null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Conversation

        if (id != other.id) return false
        if (personalKeyPair != other.personalKeyPair) return false
        if (sendChainKey != other.sendChainKey) return false
        if (receiveChainKey != other.receiveChainKey) return false
        if (contactPublicKey != other.contactPublicKey) return false
        if (lastMessageReceivedType != other.lastMessageReceivedType) return false
        if (sentLastMessageData != other.sentLastMessageData) return false
        return receivedLastMessageData == other.receivedLastMessageData
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + personalKeyPair.hashCode()
        result = 31 * result + (sendChainKey?.hashCode() ?: 0)
        result = 31 * result + (receiveChainKey?.hashCode() ?: 0)
        result = 31 * result + (contactPublicKey?.hashCode() ?: 0)
        result = 31 * result + (lastMessageReceivedType?.hashCode() ?: 0)
        result = 31 * result + (sentLastMessageData?.hashCode() ?: 0)
        result = 31 * result + (receivedLastMessageData?.hashCode() ?: 0)
        return result
    }

    enum class MessageType {
        Sent, Received
    }
}
