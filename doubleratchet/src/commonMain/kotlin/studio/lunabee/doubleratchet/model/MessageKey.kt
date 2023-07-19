package studio.lunabee.doubleratchet.model

import kotlin.jvm.JvmInline

@JvmInline
value class MessageKey(override val value: ByteArray) : DHCriticalMaterial {

    init {
        check(value.size == KEY_LENGTH_BYTE)
    }

    companion object {
        private const val KEY_LENGTH_BYTE: Int = 32

        fun empty(): MessageKey = MessageKey(ByteArray(KEY_LENGTH_BYTE))
    }
}
