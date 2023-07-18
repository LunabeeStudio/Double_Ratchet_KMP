package studio.lunabee.doubleratchet

import studio.lunabee.doubleratchet.crypto.DoubleRatchetKeyRepository
import studio.lunabee.doubleratchet.model.AsymmetricKeyPair
import studio.lunabee.doubleratchet.model.DerivedKeyPair
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
actual class TestDoubleRatchetKeyRepository actual constructor(
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
            publicKey = javaKeyPair.public.encoded.copyOf(),
            privateKey = javaKeyPair.private.encoded.copyOf(),
        )
    }

    override suspend fun generateChainKey(): ByteArray {
        return ByteArray(DEFAULT_SALT_LENGTH_BYTE).apply {
            random.nextBytes(this)
        }
    }

    override suspend fun createDiffieHellmanSharedSecret(publicKey: ByteArray, privateKey: ByteArray): ByteArray {
        val keyFactory = KeyFactory.getInstance(ALGORITHM_EC)
        val contactPublicKey = keyFactory.generatePublic(X509EncodedKeySpec(publicKey))
        val localPrivateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateKey))
        val keyAgreement = KeyAgreement.getInstance(ALGORITHM_EC_DH)
        keyAgreement.init(localPrivateKey)
        keyAgreement.doPhase(contactPublicKey, true)
        return keyAgreement.generateSecret().copyOf()
    }

    override suspend fun deriveKey(key: ByteArray): DerivedKeyPair {
        val messageKey = hashEngine.deriveKey(key, byteArrayOf(0x01)).copyOf()
        val nextChainKey = hashEngine.deriveKey(key, byteArrayOf(0x02)).copyOf()
        return DerivedKeyPair(messageKey, nextChainKey)
    }

    override suspend fun deriveKeys(chainKey: ByteArray, sharedSecret: ByteArray): DerivedKeyPair {
        val derivedWithSharedSecretKey = hashEngine.deriveKey(chainKey, sharedSecret).copyOf()
        val messageKey = hashEngine.deriveKey(derivedWithSharedSecretKey.copyOf(), byteArrayOf(0x01)).copyOf()
        val nextChainKey = hashEngine.deriveKey(derivedWithSharedSecretKey.copyOf(), byteArrayOf(0x02)).copyOf()
        return DerivedKeyPair(messageKey, nextChainKey)
    }

    private companion object {
        val hashEngine = TestPBKDF2HashEngine()

        const val DEFAULT_SALT_LENGTH_BYTE: Int = 32
        private const val NAMED_CURVE_SPEC = "secp256r1"
        private const val ALGORITHM_EC = "EC"
        private const val ALGORITHM_EC_DH = "ECDH"
    }
}
