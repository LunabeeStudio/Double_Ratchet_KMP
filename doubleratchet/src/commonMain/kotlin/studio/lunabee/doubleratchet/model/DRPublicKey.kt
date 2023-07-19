package studio.lunabee.doubleratchet.model

import kotlin.jvm.JvmInline

/**
 * Public key of an [AsymmetricKeyPair] encoded in X.509 format
 */
@JvmInline
value class DRPublicKey internal constructor(override val value: ByteArray) : DRCriticalKey {
    fun contentEquals(other: DRPublicKey?): Boolean {
        return other?.equals(value) == true
    }
}
