package studio.lunabee.doubleratchet.crypto

import studio.lunabee.doubleratchet.model.AsymmetricKeyPair
import studio.lunabee.doubleratchet.model.ChainKey
import studio.lunabee.doubleratchet.model.DerivedKeyPair
import studio.lunabee.doubleratchet.model.PrivateKey
import studio.lunabee.doubleratchet.model.PublicKey
import studio.lunabee.doubleratchet.model.SharedSecret

interface DoubleRatchetKeyRepository {
    /**
     * Generate a pair of public and a private Key
     */
    suspend fun generateKeyPair(): AsymmetricKeyPair

    /**
     * Generate a [ChainKey] for new conversation
     */
    suspend fun generateChainKey(): ChainKey

    /**
     * Generate a sharedSecret from a contact public key and a personal private key
     */
    suspend fun createDiffieHellmanSharedSecret(publicKey: PublicKey, privateKey: PrivateKey): SharedSecret

    /**
     * Derive a [ChainKey] with a Key Derivation Function and get a message key and a new chainKey
     */
    suspend fun deriveKey(key: ChainKey): DerivedKeyPair

    /**
     * Derive [ChainKey] and [SharedSecret] with a Key Derivation Function and get a message key and a new chainKey
     */
    suspend fun deriveKeys(chainKey: ChainKey, sharedSecret: SharedSecret): DerivedKeyPair
}
