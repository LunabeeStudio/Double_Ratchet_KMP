/*
 * Copyright (c) 2024 Lunabee Studio
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

package studio.lunabee.doubleratchet

import studio.lunabee.doubleratchet.model.DoubleRatchetUUID
import studio.lunabee.doubleratchet.model.toDoubleRatchetUUID
import kotlin.test.Test
import kotlin.test.assertEquals

class DoubleRatchetUUIDTest {
    private val uuidString = "db24c76f-fef6-4624-87cd-f9cf6023beaf"
    private val byteHexString = "db24c76ffef6462487cdf9cf6023beaf"

    /**
     * to assert that the byteArray conversion lead to same result on jvm and on ios
     */
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun toByteArray() {
        val uuid = DoubleRatchetUUID(uuidString)
        assertEquals(expected = uuidString, uuid.uuidString())

        val byteArray = uuid.toByteArray()
        assertEquals(expected = byteHexString, actual = byteArray.toHexString())
        val convertedUUID = byteArray.toDoubleRatchetUUID()
        assertEquals(expected = uuid, actual = convertedUUID)
    }

    @Test
    fun `construct UUID from string test`() {
        val uuidString = uuidString
        assertDoesNotThrow { DoubleRatchetUUID(uuidString) }

        val nonUuidString = "non-uuid-string"
        assertThrows<IllegalArgumentException> { DoubleRatchetUUID(nonUuidString) }
    }
}
