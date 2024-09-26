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

import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

inline fun <reified T : Throwable> assertThrows(bloc: () -> Any?): T {
    val error = runCatching(bloc).exceptionOrNull()
    assertNotNull(error)
    assertIs<T>(error)
    return error
}

inline fun <T> assertDoesNotThrow(message: String? = null, bloc: () -> T): T {
    val result = runCatching(bloc)
    val error = result.exceptionOrNull()
    assertNull(error, message ?: error?.message)
    return result.getOrThrow()
}
