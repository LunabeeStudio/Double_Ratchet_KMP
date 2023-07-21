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

// TODO does this pair makes sense vs 2 separate key (chainKey could be updated without updating the rootKey)
class DerivedKeyMessagePair(
    val chainKey: DRChainKey,
    val messageKey: DRMessageKey,
) {
    companion object {
        fun empty(): DerivedKeyMessagePair = DerivedKeyMessagePair(DRChainKey.empty(), DRMessageKey.empty())
    }
}
