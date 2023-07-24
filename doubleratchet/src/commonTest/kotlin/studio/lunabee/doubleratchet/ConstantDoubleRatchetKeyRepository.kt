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

package studio.lunabee.doubleratchet

import studio.lunabee.doubleratchet.crypto.DoubleRatchetKeyRepository
import studio.lunabee.doubleratchet.model.AsymmetricKeyPair
import studio.lunabee.doubleratchet.model.DRChainKey
import studio.lunabee.doubleratchet.model.DRMessageKey
import studio.lunabee.doubleratchet.model.DRPrivateKey
import studio.lunabee.doubleratchet.model.DRPublicKey
import studio.lunabee.doubleratchet.model.DRRootKey
import studio.lunabee.doubleratchet.model.DRSharedSecret
import studio.lunabee.doubleratchet.model.DerivedKeyMessagePair
import studio.lunabee.doubleratchet.model.DerivedKeyRootPair

/**
 * Dummy implementation of [DoubleRatchetKeyRepository] with constant arrays
 */
object ConstantDoubleRatchetKeyRepository : DoubleRatchetKeyRepository {
    override suspend fun generateKeyPair(): AsymmetricKeyPair {
        return AsymmetricKeyPair(
            publicKey = DRPublicKey(byteArrayOf(1)),
            privateKey = DRPrivateKey(byteArrayOf(2)),
        )
    }

    override suspend fun createDiffieHellmanSharedSecret(
        publicKey: DRPublicKey,
        privateKey: DRPrivateKey,
        out: DRSharedSecret,
    ): DRSharedSecret {
        out.value.indices.forEach {
            out.value[it] = 4
        }
        return out
    }

    override suspend fun deriveRootKeys(
        rootKey: DRRootKey,
        sharedSecret: DRSharedSecret,
        outRootKey: DRRootKey,
        outChainKey: DRChainKey,
    ): DerivedKeyRootPair {
        outRootKey.value.indices.forEach {
            outRootKey.value[it] = 5
        }
        outChainKey.value.indices.forEach {
            outChainKey.value[it] = 6
        }
        return DerivedKeyRootPair(outRootKey, outChainKey)
    }

    override suspend fun deriveChainKeys(
        chainKey: DRChainKey,
        outChainKey: DRChainKey,
        outMessageKey: DRMessageKey,
    ): DerivedKeyMessagePair {
        outChainKey.value.indices.forEach {
            outChainKey.value[it] = 7
        }
        outMessageKey.value.indices.forEach {
            outMessageKey.value[it] = 8
        }
        return DerivedKeyMessagePair(outChainKey, outMessageKey)
    }
}
