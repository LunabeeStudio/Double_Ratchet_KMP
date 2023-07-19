package studio.lunabee.doubleratchet.model

import kotlin.jvm.JvmInline

/**
 * Diffie Hellman generated secret using elliptic curve secp256r1
 */
@JvmInline
value class SharedSecret(override val value: ByteArray) : DHCriticalMaterial {
    companion object {
        const val DEFAULT_SECRET_LENGTH_BYTE: Int = 32

        fun empty(length: Int = DEFAULT_SECRET_LENGTH_BYTE): SharedSecret = SharedSecret(ByteArray(length))
    }
}
