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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class DoubleRatchetUUIDTest {
    private val uuidString = "db24c76f-fef6-4624-87cd-f9cf6023beaf"
    private val byteHexString = "db24c76ffef6462487cdf9cf6023beaf"

    /**
     * Asserts that the byteArray conversion lead to same result on jvm and on ios
     */
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `convert to byte array test`() {
        val uuid = Uuid.parse(uuidString)
        assertEquals(expected = uuidString, uuid.toHexDashString())

        val byteArray = uuid.toByteArray()
        assertEquals(expected = byteHexString, actual = byteArray.toHexString())
        val convertedUUID = Uuid.fromByteArray(byteArray)
        assertEquals(expected = uuid, actual = convertedUUID)
    }

    @Test
    fun `construct UUID from string test`() {
        assertDoesNotThrow { Uuid.parse(uuidString) }

        val nonUuidString = "non-uuid-string"
        assertThrows<IllegalArgumentException> { Uuid.parse(nonUuidString) }
    }
}
