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

import studio.lunabee.doubleratchet.utils.randomize

/**
 * Critical cryptographic material which must be clean after use
 */
interface DRCriticalKey {
    val value: ByteArray

    fun destroy() {
        value.randomize()
    }
}

inline fun <T : DRCriticalKey, U> T.use(block: (T) -> U): U = try {
    block(this)
} finally {
    this.destroy()
}
