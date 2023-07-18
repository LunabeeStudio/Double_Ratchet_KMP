package studio.lunabee.doubleratchet.model

class MessageHeader(
    val counter: MessageConversationCounter,
    val publicKey: PublicKey,
    val chainKey: ChainKey? = null,
)
