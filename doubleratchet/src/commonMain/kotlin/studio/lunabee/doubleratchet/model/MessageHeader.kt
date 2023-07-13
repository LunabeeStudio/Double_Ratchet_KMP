package studio.lunabee.doubleratchet.model

data class MessageHeader(
    val messageNumber: Int,
    val sequenceMessageNumber: Int,
    val publicKey: ByteArray,
    val chainKey: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MessageHeader) return false

        if (messageNumber != other.messageNumber) return false
        if (sequenceMessageNumber != other.sequenceMessageNumber) return false
        if (!publicKey.contentEquals(other.publicKey)) return false
        if (chainKey != null) {
            if (other.chainKey == null) return false
            if (!chainKey.contentEquals(other.chainKey)) return false
        } else if (other.chainKey != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = messageNumber
        result = 31 * result + sequenceMessageNumber
        result = 31 * result + publicKey.contentHashCode()
        result = 31 * result + (chainKey?.contentHashCode() ?: 0)
        return result
    }
}