package studio.lunabee.doubleratchet.crypto

import studio.lunabee.doubleratchet.model.AsymmetricKeyPair
import studio.lunabee.doubleratchet.model.DerivedKeyPair

actual class CryptoRepositoryImpl : CryptoRepository {
    override fun generateKeyPair(): AsymmetricKeyPair {
        TODO("Not yet implemented")
    }

    override fun generateChainKey(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun createDiffieHellmanSharedSecret(publicKey: ByteArray, privateKey: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }

    override fun deriveKey(key: ByteArray): DerivedKeyPair {
        TODO("Not yet implemented")
    }

    override fun deriveKeys(chainKey: ByteArray, sharedSecret: ByteArray): DerivedKeyPair {
        TODO("Not yet implemented")
    }
}