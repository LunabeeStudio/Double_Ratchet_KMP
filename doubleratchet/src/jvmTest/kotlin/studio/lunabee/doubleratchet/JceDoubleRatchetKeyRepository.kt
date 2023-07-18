package studio.lunabee.doubleratchet

import studio.lunabee.doubleratchet.crypto.DoubleRatchetKeyRepository
import studio.lunabee.doubleratchet.model.AsymmetricKeyPair
import studio.lunabee.doubleratchet.model.ChainKey
import studio.lunabee.doubleratchet.model.DerivedKeyPair
import studio.lunabee.doubleratchet.model.MessageKey
import studio.lunabee.doubleratchet.model.PrivateKey
import studio.lunabee.doubleratchet.model.PublicKey
import studio.lunabee.doubleratchet.model.SharedSecret
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
            publicKey = PublicKey(javaKeyPair.public.encoded),
            privateKey = PrivateKey(javaKeyPair.private.encoded),
        )
    }

    override suspend fun generateChainKey(): ChainKey = ChainKey.random(random)

    override suspend fun createDiffieHellmanSharedSecret(publicKey: PublicKey, privateKey: PrivateKey): SharedSecret {
        val keyFactory = KeyFactory.getInstance(ALGORITHM_EC)
        val contactPublicKey = keyFactory.generatePublic(X509EncodedKeySpec(publicKey.value))
        val localPrivateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateKey.value))
        val keyAgreement = KeyAgreement.getInstance(ALGORITHM_EC_DH)
        keyAgreement.init(localPrivateKey)
        keyAgreement.doPhase(contactPublicKey, true)
        return SharedSecret(keyAgreement.generateSecret())
    }

    override suspend fun deriveKey(key: ChainKey): DerivedKeyPair {
        val messageKey = MessageKey(hashEngine.deriveKey(key.value, byteArrayOf(0x01)))
        val nextChainKey = ChainKey(hashEngine.deriveKey(key.value, byteArrayOf(0x02)))
        return DerivedKeyPair(messageKey, nextChainKey)
    }

    override suspend fun deriveKeys(chainKey: ChainKey, sharedSecret: SharedSecret): DerivedKeyPair {
        val derivedWithSharedSecretKey = hashEngine.deriveKey(chainKey.value, sharedSecret.value)
        val messageKey = MessageKey(hashEngine.deriveKey(derivedWithSharedSecretKey, byteArrayOf(0x01)))
        val nextChainKey = ChainKey(hashEngine.deriveKey(derivedWithSharedSecretKey, byteArrayOf(0x02)))
        return DerivedKeyPair(messageKey, nextChainKey)
    }

    private companion object {
        val hashEngine = JcePBKDF2HashEngine()

        private const val NAMED_CURVE_SPEC = "secp256r1"
        private const val ALGORITHM_EC = "EC"
        private const val ALGORITHM_EC_DH = "ECDH"
    }
}
