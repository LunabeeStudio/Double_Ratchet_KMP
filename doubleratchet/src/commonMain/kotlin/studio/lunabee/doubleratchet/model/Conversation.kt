package studio.lunabee.doubleratchet.model

data class Conversation(
    val id: String,
    val sendChainKey: ByteArray? = null,
    val receiveChainKey: ByteArray? = null,
    val contactPublicKey: ByteArray? = null,
    val personalPublicKey: ByteArray,
    val personalPrivateKey: ByteArray,
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

    enum class MessageType {
        Sent, Received
    }
}

class LastMessageConversationData(
    val messageNumber: Int,
    val sequenceNumber: Int,
)