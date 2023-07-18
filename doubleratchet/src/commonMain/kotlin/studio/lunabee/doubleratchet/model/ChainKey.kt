package studio.lunabee.doubleratchet.model

import kotlin.jvm.JvmInline
import kotlin.random.Random

/**
 * Chain key used for sending (CKs) and receiving (CKr)
 *
 * @see <a href="https://signal.org/docs/specifications/doubleratchet/#state-variables">State variables</a>
 */
@JvmInline
value class ChainKey internal constructor(val value: ByteArray) {

    init {
        check(value.size == KEY_LENGTH_BYTE)
    }

    companion object {
        private const val KEY_LENGTH_BYTE: Int = 32

        /**
         * @return a random [ChainKey] using [random] param as source of randomness
         */
        fun random(random: Random): ChainKey = ChainKey(random.nextBytes(KEY_LENGTH_BYTE))
    }
}
