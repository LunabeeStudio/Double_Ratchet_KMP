package studio.lunabee.doubleratchet

import studio.lunabee.doubleratchet.crypto.DoubleRatchetKeyRepository
import studio.lunabee.doubleratchet.model.AsymmetricKeyPair
import studio.lunabee.doubleratchet.model.DerivedKeyPair

actual class TestDoubleRatchetKeyRepository : DoubleRatchetKeyRepository {
    override suspend fun generateKeyPair(): AsymmetricKeyPair {
        TODO("Not yet implemented")
    }

    override suspend fun generateChainKey(): ByteArray {
        TODO("Not yet implemented")
    }

    override suspend fun createDiffieHellmanSharedSecret(publicKey: ByteArray, privateKey: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }

    override suspend fun deriveKey(key: ByteArray): DerivedKeyPair {
        TODO("Not yet implemented")
    }

    override suspend fun deriveKeys(chainKey: ByteArray, sharedSecret: ByteArray): DerivedKeyPair {
        TODO("Not yet implemented")
    }
}