package studio.lunabee.doubleratchet.crypto

import studio.lunabee.doubleratchet.model.AsymmetricKeyPair
import studio.lunabee.doubleratchet.model.DerivedKeyPair
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement

actual class CryptoRepositoryImpl actual constructor() : CryptoRepository {
    override fun generateKeyPair(): AsymmetricKeyPair {
        val ecSpec = ECGenParameterSpec(NAMED_CURVE_SPEC)
        val keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM_EC)
        keyPairGenerator.initialize(ecSpec, SecureRandom())
        val javaKeyPair = keyPairGenerator.generateKeyPair()
        return AsymmetricKeyPair(
            publicKey = javaKeyPair.public.encoded.copyOf(),
            privateKey = javaKeyPair.private.encoded.copyOf(),
        )
    }

    override fun generateChainKey(): ByteArray {
        return ByteArray(DEFAULT_SALT_LENGTH_BYTE).apply {
            random.nextBytes(this)
        }
    }

    override fun createDiffieHellmanSharedSecret(publicKey: ByteArray, privateKey: ByteArray): ByteArray {
        val keyFactory = KeyFactory.getInstance(ALGORITHM_EC)
        val contactPublicKey = keyFactory.generatePublic(X509EncodedKeySpec(publicKey))
        val localPrivateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateKey))
        val keyAgreement = KeyAgreement.getInstance(ALGORITHM_ECDH)
        keyAgreement.init(localPrivateKey)
        keyAgreement.doPhase(contactPublicKey, true)
        return keyAgreement.generateSecret().copyOf()
    }

    override fun deriveKey(key: ByteArray): DerivedKeyPair {
        val messageKey = hashEngine.deriveKey(key, byteArrayOf(0x01)).copyOf()
        val nextChainKey = hashEngine.deriveKey(key, byteArrayOf(0x02)).copyOf()
        return DerivedKeyPair(messageKey, nextChainKey)
    }

    override fun deriveKeys(chainKey: ByteArray, sharedSecret: ByteArray): DerivedKeyPair {
        val derivedWithSharedSecretKey = hashEngine.deriveKey(chainKey, sharedSecret).copyOf()
        val messageKey = hashEngine.deriveKey(derivedWithSharedSecretKey.copyOf(), byteArrayOf(0x01)).copyOf()
        val nextChainKey = hashEngine.deriveKey(derivedWithSharedSecretKey.copyOf(), byteArrayOf(0x02)).copyOf()
        return DerivedKeyPair(messageKey, nextChainKey)
    }

    private companion object {
        val hashEngine = HashEngine()

        val random = SecureRandom()
        const val DEFAULT_SALT_LENGTH_BYTE: Int = 32
        private const val HASH_HMACSHA256 = "HmacSHA256"
        private const val NAMED_CURVE_SPEC = "secp256r1"
        private const val ALGORITHM_EC = "EC"
        private const val ALGORITHM_ECDH = "ECDH"
        private const val ALGORITHM_AES = "AES"

        private const val AES_GCM_CIPHER_TYPE = "AES/GCM/NoPadding"
        private const val AES_GCM_IV_LENGTH = 12
        private const val AES_GCM_TAG_LENGTH_IN_BITS = 128

        private const val BUFFER_SIZE = 4 * 256
    }
}