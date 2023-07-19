package studio.lunabee.doubleratchet.model

class MessageHeader(
    val counter: MessageConversationCounter,
    val publicKey: DRPublicKey,
    val chainKey: DRChainKey? = null,
)
