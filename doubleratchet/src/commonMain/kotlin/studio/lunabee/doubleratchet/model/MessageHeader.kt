package studio.lunabee.doubleratchet.model

data class MessageHeader(
    val messageNumber: Int,
    val sequenceMessageNumber: Int,
    val publicKey: ByteArray,
    val chainKey: ByteArray? = null,
)