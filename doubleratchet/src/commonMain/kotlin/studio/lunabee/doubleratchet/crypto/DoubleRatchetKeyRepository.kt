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

package studio.lunabee.doubleratchet.crypto

import studio.lunabee.doubleratchet.model.AsymmetricKeyPair
import studio.lunabee.doubleratchet.model.DRChainKey
import studio.lunabee.doubleratchet.model.DerivedKeyPair
import studio.lunabee.doubleratchet.model.DRMessageKey
import studio.lunabee.doubleratchet.model.DRPrivateKey
import studio.lunabee.doubleratchet.model.DRPublicKey
import studio.lunabee.doubleratchet.model.DRSharedSecret

interface DoubleRatchetKeyRepository {
    /**
     * Generate a pair of public and a private Key
     */
    suspend fun generateKeyPair(): AsymmetricKeyPair

    /**
     * Generate a [DRChainKey] for new conversation
     */
    suspend fun generateChainKey(): DRChainKey

    /**
     * Generate a [DRSharedSecret] from a contact public key and a personal private key in param array [out]
     */
    suspend fun createDiffieHellmanSharedSecret(
        publicKey: DRPublicKey,
        privateKey: DRPrivateKey,
        out: DRSharedSecret = DRSharedSecret.empty(),
    ): DRSharedSecret

    /**
     * Derive a [DRChainKey] with a Key Derivation Function and get a message key and a new chainKey
     */
    suspend fun deriveKey(
        key: DRChainKey,
        out: DerivedKeyPair = DerivedKeyPair(DRMessageKey.empty(), DRChainKey.empty()),
    ): DerivedKeyPair

    /**
     * Derive [DRChainKey] and [DRSharedSecret] with a Key Derivation Function and get a message key and a new chainKey
     */
    suspend fun deriveKeys(
        chainKey: DRChainKey,
        sharedSecret: DRSharedSecret,
        out: DerivedKeyPair = DerivedKeyPair(DRMessageKey.empty(), DRChainKey.empty()),
    ): DerivedKeyPair
}
