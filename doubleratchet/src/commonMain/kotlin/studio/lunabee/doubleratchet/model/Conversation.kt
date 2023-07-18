package studio.lunabee.doubleratchet.model

data class Conversation(
    val id: DoubleRatchetUUID,
    val personalPublicKey: ByteArray,
    val personalPrivateKey: ByteArray,
    val sendChainKey: ChainKey? = null,
    val receiveChainKey: ChainKey? = null,
    val contactPublicKey: ByteArray? = null,
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
        if (!personalPublicKey.contentEquals(other.personalPublicKey)) return false
        if (!personalPrivateKey.contentEquals(other.personalPrivateKey)) return false
        if (sendChainKey != other.sendChainKey) return false
        if (receiveChainKey != other.receiveChainKey) return false
        if (contactPublicKey != null) {
            if (other.contactPublicKey == null) return false
            if (!contactPublicKey.contentEquals(other.contactPublicKey)) return false
        } else if (other.contactPublicKey != null) return false
        if (lastMessageReceivedType != other.lastMessageReceivedType) return false
        if (sentLastMessageData != other.sentLastMessageData) return false
        return receivedLastMessageData == other.receivedLastMessageData
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + personalPublicKey.contentHashCode()
        result = 31 * result + personalPrivateKey.contentHashCode()
        result = 31 * result + (sendChainKey?.hashCode() ?: 0)
        result = 31 * result + (receiveChainKey?.hashCode() ?: 0)
        result = 31 * result + (contactPublicKey?.contentHashCode() ?: 0)
        result = 31 * result + (lastMessageReceivedType?.hashCode() ?: 0)
        result = 31 * result + (sentLastMessageData?.hashCode() ?: 0)
        result = 31 * result + (receivedLastMessageData?.hashCode() ?: 0)
        return result
    }

    enum class MessageType {
        Sent, Received
    }
}
