package studio.lunabee.doubleratchet.model

import kotlin.jvm.JvmInline

@JvmInline
value class MessageKey(override val value: ByteArray) : DHCriticalMaterial {

    companion object {
        private const val DEFAULT_KEY_LENGTH_BYTE: Int = 32

        fun empty(length: Int = DEFAULT_KEY_LENGTH_BYTE): MessageKey = MessageKey(ByteArray(length))
    }
}
