package studio.lunabee.doubleratchet.model

class DerivedKeyPair(
    val messageKey: MessageKey,
    val chainKey: ChainKey,
) {
    companion object {
        fun empty(): DerivedKeyPair = DerivedKeyPair(MessageKey.empty(), ChainKey.empty())
    }
}
