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
