package studio.lunabee.doubleratchet.model

import kotlin.jvm.JvmInline

@JvmInline
value class DRMessageKey(override val value: ByteArray) : DRCriticalKey {

    companion object {
        private const val DEFAULT_KEY_LENGTH_BYTE: Int = 32

        fun empty(length: Int = DEFAULT_KEY_LENGTH_BYTE): DRMessageKey = DRMessageKey(ByteArray(length))
    }
}
