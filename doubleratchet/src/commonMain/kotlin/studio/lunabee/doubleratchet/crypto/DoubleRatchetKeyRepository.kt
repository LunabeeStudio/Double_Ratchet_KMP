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
import studio.lunabee.doubleratchet.model.DRMessageKey
import studio.lunabee.doubleratchet.model.DRPrivateKey
import studio.lunabee.doubleratchet.model.DRPublicKey
import studio.lunabee.doubleratchet.model.DRRootKey
import studio.lunabee.doubleratchet.model.DRSharedSecret
import studio.lunabee.doubleratchet.model.DerivedKeyMessagePair
import studio.lunabee.doubleratchet.model.DerivedKeyRootPair

interface DoubleRatchetKeyRepository {
    /**
     * Generate a pair of public and a private Key
     */
    suspend fun generateKeyPair(): AsymmetricKeyPair

    /**
     * Generate a [DRSharedSecret] from a contact public key and a personal private key in param array [out]
     */
    suspend fun createDiffieHellmanSharedSecret(
        publicKey: DRPublicKey,
        privateKey: DRPrivateKey,
        out: DRSharedSecret = DRSharedSecret.empty(sharedSecretByteSize),
    ): DRSharedSecret

    /**
     * KDF_RK(rk, dh_out)
     */
    suspend fun deriveRootKeys(
        rootKey: DRRootKey,
        sharedSecret: DRSharedSecret,
        outRootKey: DRRootKey = DRRootKey.empty(rootKeyByteSize),
        outChainKey: DRChainKey = DRChainKey.empty(chainKeyByteSize),
    ): DerivedKeyRootPair

    /**
     * KDF_CK(ck, ck_out)
     */
    suspend fun deriveChainKeys(
        chainKey: DRChainKey,
        outChainKey: DRChainKey = DRChainKey.empty(chainKeyByteSize),
        outMessageKey: DRMessageKey = DRMessageKey.empty(messageKeyByteSize),
    ): DerivedKeyMessagePair

    /**
     * Message key size in bytes. Default 32
     */
    val messageKeyByteSize: Int
        get() = 32

    /**
     * Chain key size in bytes. Default 32
     */
    val chainKeyByteSize: Int
        get() = 32

    /**
     * Root key size in bytes. Default 32
     */
    val rootKeyByteSize: Int
        get() = 32

    /**
     * Root key size in bytes. Default 32
     */
    val sharedSecretByteSize: Int
        get() = 32
}
