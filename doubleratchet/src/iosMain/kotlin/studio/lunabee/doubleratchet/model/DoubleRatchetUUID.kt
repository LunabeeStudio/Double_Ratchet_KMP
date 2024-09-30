/*
 * Copyright (c) 2023-2023 Lunabee Studio
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

import platform.Foundation.NSUUID

actual class DoubleRatchetUUID(val uuid: NSUUID) {
    actual fun uuidString(): String = uuid.UUIDString().lowercase()

    override fun equals(other: Any?): Boolean {
        return other is DoubleRatchetUUID && other.uuid.isEqual(uuid)
    }

    override fun hashCode(): Int = uuid.UUIDString().hashCode()

    override fun toString(): String = uuidString()

    actual fun toByteArray(): ByteArray {
        val uuidCharRange: List<IntRange> = listOf(
            0 until 8,
            9 until 13,
            14 until 18,
            19 until 23,
            24 until 36,
        )
        val bytes = ByteArray(16)
        var byte = 0
        for (range in uuidCharRange) {
            var i = range.first
            while (i < range.last) {
                // Collect each pair of UUID chars and their int representations
                val left = halfByteFromChar(uuidString()[i++])
                val right = halfByteFromChar(uuidString()[i++])
                require(left != null && right != null) {
                    "Uuid string has invalid characters: ${uuidString()}"
                }

                // smash them together into a single byte
                bytes[byte++] = (left.shl(4) or right).toByte()
            }
        }
        return bytes
    }

    actual companion object {
        @Throws(IllegalArgumentException::class)
        actual fun fromString(uuidString: String): DoubleRatchetUUID {
            val uuid = kotlin.runCatching { NSUUID(uuidString) }.getOrNull()
                ?: throw IllegalArgumentException("Invalid UUID string")
            return DoubleRatchetUUID(uuid = uuid)
        }
    }
}

actual fun createRandomUUID(): DoubleRatchetUUID = DoubleRatchetUUID(NSUUID())

/**
inspired by https://github.com/benasher44/uuid
 */
actual fun ByteArray.toDoubleRatchetUUID(): DoubleRatchetUUID {
    val uuidByteRanges: List<IntRange> = listOf(
        0 until 4,
        4 until 6,
        6 until 8,
        8 until 10,
        10 until 16,
    )
    val uuidChars = ('0'..'9') + ('a'..'f')
    val characters = CharArray(36)
    var charIndex = 0
    for (range in uuidByteRanges) {
        for (i in range) {
            val octetPair = this[i]
            // convert the octet pair in this byte into 2 characters
            val left = octetPair.toInt().shr(4) and 0b00001111
            val right = octetPair.toInt() and 0b00001111
            characters[charIndex++] = uuidChars[left]
            characters[charIndex++] = uuidChars[right]
        }
        if (charIndex < 36) {
            characters[charIndex++] = '-'
        }
    }
    return DoubleRatchetUUID.fromString(characters.concatToString())
}

/**
inspired by https://github.com/benasher44/uuid
 */
private fun halfByteFromChar(char: Char) = when (char) {
    in '0'..'9' -> char.code - 48
    in 'a'..'f' -> char.code - 87
    in 'A'..'F' -> char.code - 55
    else -> null
}
