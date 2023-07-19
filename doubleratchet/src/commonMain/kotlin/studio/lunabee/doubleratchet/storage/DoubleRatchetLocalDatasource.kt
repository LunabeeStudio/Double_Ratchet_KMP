package studio.lunabee.doubleratchet.storage

import studio.lunabee.doubleratchet.model.DRChainKey
import studio.lunabee.doubleratchet.model.Conversation
import studio.lunabee.doubleratchet.model.DoubleRatchetUUID
import studio.lunabee.doubleratchet.model.DRMessageKey

/**
 * Used by the doubleRatchet engine to store and retrieve the keys it needs.
 */
interface DoubleRatchetLocalDatasource {
    suspend fun saveOrUpdateConversation(conversation: Conversation)
    suspend fun getConversation(id: DoubleRatchetUUID): Conversation?
    suspend fun saveMessageKey(id: String, key: DRMessageKey)

    /**
     * Save the initial chain key
     */
    suspend fun saveChainKey(id: DoubleRatchetUUID, key: DRChainKey)

    /**
     * Return and delete [DRMessageKey] with id [id]
     */
    suspend fun popMessageKey(id: String): DRMessageKey?

    /**
     * @return The initial chain key for initialization (null after initialization done)
     */
    suspend fun retrieveChainKey(id: DoubleRatchetUUID): DRChainKey?

    /**
     * Delete the initial chain key
     */
    suspend fun deleteChainKey(id: DoubleRatchetUUID)
}
