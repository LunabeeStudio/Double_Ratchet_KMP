package studio.lunabee.doubleratchet.model

import kotlin.jvm.JvmInline
import kotlin.random.Random

@JvmInline
value class ChainKey internal constructor(val value: ByteArray) {

    init {
        check(value.size == KEY_LENGTH_BYTE)
    }

    companion object {
        private const val KEY_LENGTH_BYTE: Int = 32

        fun random(random: Random): ChainKey = ChainKey(random.nextBytes(KEY_LENGTH_BYTE))
    }
}
