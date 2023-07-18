package studio.lunabee.doubleratchet.model

import kotlin.jvm.JvmInline

@JvmInline
value class MessageKey(val value: ByteArray) {

    init {
        check(value.size == KEY_LENGTH_BYTE)
    }

    companion object {
        private const val KEY_LENGTH_BYTE: Int = 32
    }
}
