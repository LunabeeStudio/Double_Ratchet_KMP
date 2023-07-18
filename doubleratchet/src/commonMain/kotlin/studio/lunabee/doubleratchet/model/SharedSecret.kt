package studio.lunabee.doubleratchet.model

import kotlin.jvm.JvmInline

@JvmInline
value class SharedSecret(val value: ByteArray) {
    init {
        check(value.size >= 0) // TODO size check
    }
}
