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

/**
 * Container for the pair of keys returned by the [KDF_RK][studio.lunabee.doubleratchet.crypto.DoubleRatchetKeyRepository.deriveRootKeys]
 * function
 *
 * @property rootKey the derived root key
 * @property chainKey the derived chain key
 */
class DerivedKeyRootPair(
    val rootKey: DRRootKey,
    val chainKey: DRChainKey,
)
