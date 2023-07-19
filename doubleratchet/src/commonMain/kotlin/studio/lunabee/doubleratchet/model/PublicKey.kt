package studio.lunabee.doubleratchet.model

import kotlin.jvm.JvmInline

/**
 * Public key of an [AsymmetricKeyPair] encoded in X.509 format
 */
@JvmInline
value class PublicKey internal constructor(override val value: ByteArray) : DHCriticalMaterial {
    fun contentEquals(other: PublicKey?): Boolean {
        return other?.equals(value) == true
    }
}
