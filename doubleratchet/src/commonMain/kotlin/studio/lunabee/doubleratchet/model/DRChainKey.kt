package studio.lunabee.doubleratchet.model

import kotlin.jvm.JvmInline
import kotlin.random.Random

/**
 * Chain key used for sending (CKs) and receiving (CKr)
 *
 * @see <a href="https://signal.org/docs/specifications/doubleratchet/#state-variables">State variables</a>
 */
@JvmInline
value class DRChainKey internal constructor(override val value: ByteArray) : DRCriticalKey {
    companion object {
        private const val DEFAULT_KEY_LENGTH_BYTE: Int = 32

        /**
         * @return a random [DRChainKey] using [random] param as source of randomness
         */
        fun random(random: Random, length: Int = DEFAULT_KEY_LENGTH_BYTE): DRChainKey = DRChainKey(random.nextBytes(length))

        fun empty(length: Int = DEFAULT_KEY_LENGTH_BYTE): DRChainKey = DRChainKey(ByteArray(length))
    }
}
