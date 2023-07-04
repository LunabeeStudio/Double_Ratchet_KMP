package studio.lunabee.doubleratchet.storage

import studio.lunabee.doubleratchet.model.Conversation

/**
 * Used by the doubleRatchet engine to store and retrieve the keys it needs.
 */
interface DoubleRatchetLocalDatasource {
    suspend fun saveOrUpdateConversation(conversation: Conversation)
    suspend fun getConversation(id: String): Conversation?
    suspend fun saveMessageKey(id: String, key: ByteArray)
    suspend fun retrieveMessageKey(id: String): ByteArray?
    suspend fun deleteMessageKey(id: String)
}