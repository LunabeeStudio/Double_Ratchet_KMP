package studio.lunabee.doubleratchet.model

import kotlin.jvm.JvmInline
import kotlin.random.Random

/**
 * Chain key used for sending (CKs) and receiving (CKr)
 *
 * @see <a href="https://signal.org/docs/specifications/doubleratchet/#state-variables">State variables</a>
 */
@JvmInline
value class ChainKey internal constructor(override val value: ByteArray) : DHCriticalMaterial {
    companion object {
        private const val DEFAULT_KEY_LENGTH_BYTE: Int = 32

        /**
         * @return a random [ChainKey] using [random] param as source of randomness
         */
        fun random(random: Random, length: Int = DEFAULT_KEY_LENGTH_BYTE): ChainKey = ChainKey(random.nextBytes(length))

        fun empty(length: Int = DEFAULT_KEY_LENGTH_BYTE): ChainKey = ChainKey(ByteArray(length))
    }
}
