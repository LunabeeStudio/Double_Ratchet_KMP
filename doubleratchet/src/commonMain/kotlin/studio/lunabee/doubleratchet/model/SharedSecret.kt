package studio.lunabee.doubleratchet.model

import kotlin.jvm.JvmInline

/**
 * Diffie Hellman generated secret using elliptic curve secp256r1
 */
@JvmInline
value class SharedSecret(val value: ByteArray) {
    init {
        check(value.size == SECRET_LENGTH_BYTE)
    }

    companion object {
        const val SECRET_LENGTH_BYTE: Int = 32
    }
}
