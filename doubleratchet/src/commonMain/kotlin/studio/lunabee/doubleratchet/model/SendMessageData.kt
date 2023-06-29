package studio.lunabee.doubleratchet.model

data class SendMessageData(
    val messageHeader: MessageHeader,
    val messageKey: ByteArray,
)