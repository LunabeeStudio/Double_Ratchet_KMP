package studio.lunabee.doubleratchet.model

import kotlin.jvm.JvmInline

/**
 * Wrapper for Signal protocol message number (N) and chain length (PN)
 *
 * @see <a href="https://signal.org/docs/specifications/doubleratchet/#state-variables">State variables</a>
 */
@JvmInline
value class MessageConversationCounter private constructor(private val value: ULong) {
    constructor(message: UInt, sequence: UInt) :
        this(message.toULong() shl 32 or (sequence.toULong() and 0xffffffffu))

    /**
     * Message number (N)
     */
    val message: UInt
        get() = (value shr 32).toUInt()

    /**
     * Chain length (PN)
     */
    val sequence: UInt
        get() = value.toUInt()
}
