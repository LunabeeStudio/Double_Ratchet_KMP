package studio.lunabee.doubleratchet

import kotlin.test.assertNotNull
import kotlin.test.asserter

fun assertIsEmpty(actual: Map<*, *>? = null, message: String? = null) {
    assertNotNull(actual)
    return asserter.assertTrue(message ?: "The list is not empty (${actual.size} elements)\n$actual", actual.isEmpty())
}
