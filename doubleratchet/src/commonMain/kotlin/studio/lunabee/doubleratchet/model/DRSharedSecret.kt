package studio.lunabee.doubleratchet.model

import kotlin.jvm.JvmInline

/**
 * Diffie Hellman generated secret using elliptic curve secp256r1
 */
@JvmInline
value class DRSharedSecret(override val value: ByteArray) : DRCriticalKey {
    companion object {
        const val DEFAULT_SECRET_LENGTH_BYTE: Int = 32

        fun empty(length: Int = DEFAULT_SECRET_LENGTH_BYTE): DRSharedSecret = DRSharedSecret(ByteArray(length))
    }
}
