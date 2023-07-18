package studio.lunabee.doubleratchet.storage

import studio.lunabee.doubleratchet.model.ChainKey
import studio.lunabee.doubleratchet.model.Conversation
import studio.lunabee.doubleratchet.model.DoubleRatchetUUID
import studio.lunabee.doubleratchet.model.MessageKey

/**
 * Used by the doubleRatchet engine to store and retrieve the keys it needs.
 */
interface DoubleRatchetLocalDatasource {
    suspend fun saveOrUpdateConversation(conversation: Conversation)
    suspend fun getConversation(id: DoubleRatchetUUID): Conversation?
    suspend fun saveMessageKey(id: String, key: MessageKey)

    /**
     * Save the initial chain key
     */
    suspend fun saveChainKey(id: String, key: ChainKey)

    /**
     * Return and delete [MessageKey] with id [id]
     */
    suspend fun popMessageKey(id: String): MessageKey?
    suspend fun retrieveChainKey(id: String): ChainKey?

    /**
     * Delete the initial chain key
     */
    suspend fun deleteChainKey(id: String)
}
