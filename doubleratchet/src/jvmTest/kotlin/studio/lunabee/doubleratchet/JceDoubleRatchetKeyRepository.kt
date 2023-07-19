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
import studio.lunabee.doubleratchet.model.DerivedKeyPair
import studio.lunabee.doubleratchet.model.DRPrivateKey
import studio.lunabee.doubleratchet.model.DRPublicKey
import studio.lunabee.doubleratchet.model.DRSharedSecret
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement
import kotlin.random.Random

/**
 * Implementation only provided for testing,
 * Use ECDH to create the sharedSecrets
 * Use PBKDF2 with HmacSHA512 to derive keys
 */
class JceDoubleRatchetKeyRepository(
    private val random: Random,
) : DoubleRatchetKeyRepository {
    private val secureRandom = SecureRandom.getInstance("SHA1PRNG").apply {
        setSeed(random.nextLong())
    }

    override suspend fun generateKeyPair(): AsymmetricKeyPair {
        val ecSpec = ECGenParameterSpec(NAMED_CURVE_SPEC)
        val keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM_EC)
        keyPairGenerator.initialize(ecSpec, secureRandom)
        val javaKeyPair = keyPairGenerator.generateKeyPair()
        return AsymmetricKeyPair(
            publicKey = DRPublicKey(javaKeyPair.public.encoded),
            privateKey = DRPrivateKey(javaKeyPair.private.encoded),
        )
    }

    override suspend fun generateChainKey(): DRChainKey = DRChainKey.random(random)

    override suspend fun createDiffieHellmanSharedSecret(
        publicKey: DRPublicKey,
        privateKey: DRPrivateKey,
        out: DRSharedSecret,
    ): DRSharedSecret {
        val keyFactory = KeyFactory.getInstance(ALGORITHM_EC)
        val contactPublicKey = keyFactory.generatePublic(X509EncodedKeySpec(publicKey.value))
        val localPrivateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateKey.value))
        val keyAgreement = KeyAgreement.getInstance(ALGORITHM_EC_DH)
        keyAgreement.init(localPrivateKey)
        keyAgreement.doPhase(contactPublicKey, true)
        keyAgreement.generateSecret(out.value, 0)
        return out
    }

    override suspend fun deriveKey(key: DRChainKey, out: DerivedKeyPair): DerivedKeyPair {
        hashEngine.deriveKey(key.value, messageSalt, out.messageKey.value)
        hashEngine.deriveKey(key.value, chainSalt, out.chainKey.value)
        return out
    }

    override suspend fun deriveKeys(chainKey: DRChainKey, sharedSecret: DRSharedSecret, out: DerivedKeyPair): DerivedKeyPair {
        val derivedWithSharedSecretKey = hashEngine.deriveKey(chainKey.value, sharedSecret.value)
        hashEngine.deriveKey(derivedWithSharedSecretKey, messageSalt, out.messageKey.value)
        hashEngine.deriveKey(derivedWithSharedSecretKey, chainSalt, out.chainKey.value)
        return out
    }

    private companion object {
        val hashEngine = BouncyCastleHKDFHashEngine()

        private const val NAMED_CURVE_SPEC = "secp256r1"
        private const val ALGORITHM_EC = "EC"
        private const val ALGORITHM_EC_DH = "ECDH"

        private val messageSalt = byteArrayOf(0x01)
        private val chainSalt = byteArrayOf(0x02)
    }
}
