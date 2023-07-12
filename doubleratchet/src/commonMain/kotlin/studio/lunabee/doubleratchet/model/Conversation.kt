package studio.lunabee.doubleratchet.model

data class Conversation(
    val id: DoubleRatchetUUID,
    val personalPublicKey: ByteArray,
    val personalPrivateKey: ByteArray,
    val sendChainKey: ByteArray? = null,
    val receiveChainKey: ByteArray? = null,
    val contactPublicKey: ByteArray? = null,
    val lastMessageReceivedType: MessageType? = null,
    val sentLastMessageData: LastMessageConversationData? = null,
    val receivedLastMessageData: LastMessageConversationData? = null,
) {
    fun isReadyForMessageSending(): Boolean {
        return sendChainKey != null && contactPublicKey != null
    }

    fun isReadyForMessageReceiving(): Boolean {
        return receiveChainKey != null && contactPublicKey != null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Conversation) return false

        if (id != other.id) return false
        if (!personalPublicKey.contentEquals(other.personalPublicKey)) return false
        if (!personalPrivateKey.contentEquals(other.personalPrivateKey)) return false
        if (sendChainKey != null) {
            if (other.sendChainKey == null) return false
            if (!sendChainKey.contentEquals(other.sendChainKey)) return false
        } else if (other.sendChainKey != null) return false
        if (receiveChainKey != null) {
            if (other.receiveChainKey == null) return false
            if (!receiveChainKey.contentEquals(other.receiveChainKey)) return false
        } else if (other.receiveChainKey != null) return false
        if (contactPublicKey != null) {
            if (other.contactPublicKey == null) return false
            if (!contactPublicKey.contentEquals(other.contactPublicKey)) return false
        } else if (other.contactPublicKey != null) return false
        if (lastMessageReceivedType != other.lastMessageReceivedType) return false
        if (sentLastMessageData != other.sentLastMessageData) return false
        if (receivedLastMessageData != other.receivedLastMessageData) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + personalPublicKey.contentHashCode()
        result = 31 * result + personalPrivateKey.contentHashCode()
        result = 31 * result + (sendChainKey?.contentHashCode() ?: 0)
        result = 31 * result + (receiveChainKey?.contentHashCode() ?: 0)
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

class LastMessageConversationData(
    val messageNumber: Int,
    val sequenceNumber: Int,
)