package studio.lunabee.doubleratchet.model

class DerivedKeyPair(
    val messageKey: ByteArray,
    val nextChainKey: ByteArray,
)