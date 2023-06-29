package studio.lunabee.doubleratchet.crypto

import studio.lunabee.doubleratchet.model.AsymmetricKeyPair
import studio.lunabee.doubleratchet.model.DerivedKeyPair

interface CryptoRepository {
    /**
     * Generate a public and a private Key
     */
    fun generateKeyPair(): AsymmetricKeyPair

    /**
     * generate a chainKey for new conversation
     */
    fun generateChainKey(): ByteArray

    /**
     * Generate a sharedSecret from a contact public key and a personal private key
     */
    fun createDiffieHellmanSharedSecret(publicKey: ByteArray, privateKey: ByteArray): ByteArray

    /**
     * Derive chainKey with a Key Derivation Function and get a message key and a new chainKey
     */
    fun deriveKey(key: ByteArray): DerivedKeyPair

    /**
     * Derive chainKey and sharedSecret with a Key Derivation Function and get a message key and a new chainKey
     */
    fun deriveKeys(chainKey: ByteArray, sharedSecret: ByteArray): DerivedKeyPair
}