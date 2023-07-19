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
import kotlin.random.Random

/**
 * Chain key used for sending (CKs) and receiving (CKr)
 *
 * @see <a href="https://signal.org/docs/specifications/doubleratchet/#state-variables">State variables</a>
 */
@JvmInline
value class DRChainKey internal constructor(override val value: ByteArray) : DRCriticalKey {
    companion object {
        private const val DEFAULT_KEY_LENGTH_BYTE: Int = 32

        /**
         * @return a random [DRChainKey] using [random] param as source of randomness
         */
        fun random(random: Random, length: Int = DEFAULT_KEY_LENGTH_BYTE): DRChainKey = DRChainKey(random.nextBytes(length))

        fun empty(length: Int = DEFAULT_KEY_LENGTH_BYTE): DRChainKey = DRChainKey(ByteArray(length))
    }
}