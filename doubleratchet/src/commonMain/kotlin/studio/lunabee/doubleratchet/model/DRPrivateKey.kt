package studio.lunabee.doubleratchet.model

import kotlin.jvm.JvmInline

/**
 * Private key of an [AsymmetricKeyPair] encoded in PKCS #8 format
 */
@JvmInline
value class DRPrivateKey internal constructor(override val value: ByteArray) : DRCriticalKey
