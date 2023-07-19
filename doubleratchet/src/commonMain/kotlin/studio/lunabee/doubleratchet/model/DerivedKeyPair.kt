package studio.lunabee.doubleratchet.model

class DerivedKeyPair(
    val messageKey: DRMessageKey,
    val chainKey: DRChainKey,
) {
    companion object {
        fun empty(): DerivedKeyPair = DerivedKeyPair(DRMessageKey.empty(), DRChainKey.empty())
    }
}
