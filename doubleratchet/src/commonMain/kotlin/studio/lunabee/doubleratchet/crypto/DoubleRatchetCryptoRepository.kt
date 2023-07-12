package studio.lunabee.doubleratchet.crypto

import studio.lunabee.doubleratchet.model.AsymmetricKeyPair
import studio.lunabee.doubleratchet.model.DerivedKeyPair

interface DoubleRatchetCryptoRepository {
    /**
     * Generate a pair of public and a private Key
     */
    suspend fun generateKeyPair(): AsymmetricKeyPair

    /**
     * Generate a chainKey for new conversation
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