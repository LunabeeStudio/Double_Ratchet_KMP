package studio.lunabee.doubleratchet.model

class MessageHeader(
    val counter: MessageConversationCounter,
    val publicKey: ByteArray,
    val chainKey: ChainKey? = null,
)
