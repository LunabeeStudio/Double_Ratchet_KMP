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
class ConstantDoubleRatchetKeyRepository(keyLength: Int) : DoubleRatchetKeyRepository {

    override val messageKeyByteSize: Int = keyLength
    override val chainKeyByteSize: Int = keyLength
    override val rootKeyByteSize: Int = keyLength
    override val sharedSecretByteSize: Int = keyLength

    companion object {
        val rootKeys: List<ByteArray> = List(100) {
            RandomProviderTest.random.nextBytes(ConversationTest.keyLength)
        }
    }

    private var rootCounter: Int = 0

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
        out.value.fill(3)
        return out
    }

    override suspend fun deriveRootKeys(
        rootKey: DRRootKey,
        sharedSecret: DRSharedSecret,
        outRootKey: DRRootKey,
        outChainKey: DRChainKey,
    ): DerivedKeyRootPair {
        rootKeys[rootCounter++].copyInto(outRootKey.value)
        return DerivedKeyRootPair(outRootKey, outChainKey)
    }

    override suspend fun deriveChainKeys(
        chainKey: DRChainKey,
        outChainKey: DRChainKey,
        outMessageKey: DRMessageKey,
    ): DerivedKeyMessagePair {
        outMessageKey.value.fill(4)
        return DerivedKeyMessagePair(outChainKey, outMessageKey)
    }
}
