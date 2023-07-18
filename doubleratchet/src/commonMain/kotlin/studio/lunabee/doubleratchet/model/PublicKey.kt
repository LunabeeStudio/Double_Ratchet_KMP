package studio.lunabee.doubleratchet.model

import kotlin.jvm.JvmInline

/**
 * Public key of an [AsymmetricKeyPair] encoded in X.509 format
 */
@JvmInline
value class PublicKey internal constructor(val value: ByteArray) {
    fun contentEquals(other: PublicKey?): Boolean {
        return other?.equals(value) == true
    }

    init {
        check(value.size == KEY_LENGTH_BYTE)
    }

    companion object {
        private const val KEY_LENGTH_BYTE: Int = 91
    }
}
