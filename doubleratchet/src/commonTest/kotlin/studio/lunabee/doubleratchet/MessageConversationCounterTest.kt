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

package studio.lunabee.doubleratchet

import studio.lunabee.doubleratchet.model.MessageConversationCounter
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageConversationCounterTest {

    companion object {
        private val random: Random = RandomProviderTest.random
    }

    @Test
    fun `Random counter instantiation test`() {
        repeat(100) {
            val (a, b) = random.nextUInt() to random.nextUInt()
            val counter = MessageConversationCounter(a, b)

            assertEquals(a, counter.message)
            assertEquals(b, counter.sequence)
        }
    }
}
