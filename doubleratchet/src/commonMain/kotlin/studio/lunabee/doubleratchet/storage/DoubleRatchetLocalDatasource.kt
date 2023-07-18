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
    suspend fun saveChainKey(id: String, key: ChainKey)
    suspend fun retrieveMessageKey(id: String): MessageKey?
    suspend fun retrieveChainKey(id: String): ChainKey?
    suspend fun deleteMessageKey(id: String)
}
