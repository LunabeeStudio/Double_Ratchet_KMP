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

package studio.lunabee.doubleratchet.crypto

import studio.lunabee.doubleratchet.model.AsymmetricKeyPair
import studio.lunabee.doubleratchet.model.DerivedKeyPair

interface DoubleRatchetCryptoRepository {
    /**
     * Generate a public and a private Key
     */
    suspend fun generateKeyPair(): AsymmetricKeyPair

    /**
     * generate a chainKey for new conversation
     */
    suspend fun generateChainKey(): ByteArray

    /**
     * Generate a sharedSecret from a contact public key and a personal private key
     */
    suspend fun createDiffieHellmanSharedSecret(publicKey: ByteArray, privateKey: ByteArray): ByteArray

    /**
     * Derive chainKey with a Key Derivation Function and get a message key and a new chainKey
     */
    suspend fun deriveKey(key: ByteArray): DerivedKeyPair

    /**
     * Derive chainKey and sharedSecret with a Key Derivation Function and get a message key and a new chainKey
     */
    suspend fun deriveKeys(chainKey: ByteArray, sharedSecret: ByteArray): DerivedKeyPair
}