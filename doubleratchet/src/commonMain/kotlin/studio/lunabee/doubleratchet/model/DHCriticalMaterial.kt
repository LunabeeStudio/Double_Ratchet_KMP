package studio.lunabee.doubleratchet.model

import studio.lunabee.doubleratchet.utils.randomize

/**
 * Critical cryptographic material which must be clean after use
 */
interface DHCriticalMaterial {
    val value: ByteArray

    fun destroy() {
        value.randomize()
    }
}

inline fun <T : DHCriticalMaterial, U> T.use(block: (T) -> U): U = try {
    block(this)
} finally {
    this.destroy()
}
