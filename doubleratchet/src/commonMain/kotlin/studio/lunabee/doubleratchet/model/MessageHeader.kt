package studio.lunabee.doubleratchet.model

data class MessageHeader(
    val counter: MessageConversationCounter,
    val publicKey: ByteArray,
    val chainKey: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as MessageHeader

        if (counter != other.counter) return false
        if (!publicKey.contentEquals(other.publicKey)) return false
        if (chainKey != null) {
            if (other.chainKey == null) return false
            if (!chainKey.contentEquals(other.chainKey)) return false
        } else if (other.chainKey != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = counter.hashCode()
        result = 31 * result + publicKey.contentHashCode()
        result = 31 * result + (chainKey?.contentHashCode() ?: 0)
        return result
    }
}
