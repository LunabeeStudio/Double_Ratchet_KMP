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

import java.nio.ByteBuffer
import java.util.UUID

actual class DoubleRatchetUUID(val uuid: UUID) {
    actual fun uuidString(): String = uuid.toString()

    override fun equals(other: Any?): Boolean {
        return other is DoubleRatchetUUID && uuid == other.uuid
    }

    override fun hashCode(): Int = uuid.toString().hashCode()

    actual companion object {
        @Throws(IllegalArgumentException::class)
        actual operator fun invoke(string: String): DoubleRatchetUUID {
            return DoubleRatchetUUID(uuid = UUID.fromString(string))
        }
    }

    override fun toString(): String = uuidString()

    actual fun toByteArray(): ByteArray {
        val bytes = ByteArray(16)
        val buffer = ByteBuffer.wrap(bytes)
        buffer.putLong(uuid.mostSignificantBits)
        buffer.putLong(uuid.leastSignificantBits)
        return buffer.array()
    }
}

actual fun createRandomUUID(): DoubleRatchetUUID {
    return DoubleRatchetUUID(uuid = UUID.randomUUID())
}

actual fun ByteArray.toDoubleRatchetUUID(): DoubleRatchetUUID {
    val buffer = ByteBuffer.wrap(this)
    val firstLong = buffer.long
    val secondLong = buffer.long
    return DoubleRatchetUUID(UUID(firstLong, secondLong))
}
