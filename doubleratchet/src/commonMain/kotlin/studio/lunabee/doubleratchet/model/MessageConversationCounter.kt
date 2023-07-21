/*
 * Copyright (c) 2023 Lunabee Studio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package studio.lunabee.doubleratchet.model

import kotlin.jvm.JvmInline

// TODO too overkill? or add a way to update message and sequence separately

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
