package studio.lunabee.doubleratchet.model

import kotlin.jvm.JvmInline

/**
 * Private key of an [AsymmetricKeyPair] encoded in PKCS #8 format
 */
@JvmInline
value class PrivateKey internal constructor(val value: ByteArray) {

    init {
        check(value.size == KEY_LENGTH_BYTE)
    }

    companion object {
        private const val KEY_LENGTH_BYTE: Int = 67
    }
}
