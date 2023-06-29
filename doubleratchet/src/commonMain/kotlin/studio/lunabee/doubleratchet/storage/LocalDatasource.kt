package studio.lunabee.doubleratchet.storage

import studio.lunabee.doubleratchet.model.Conversation

/**
 * Used by the doubleRatchet engine to store and retrieve the keys it needs.
 */
interface LocalDatasource {
    fun saveOrUpdateConversation(conversation: Conversation)
    fun getConversation(id: String): Conversation?
    fun saveMessageKey(id: String, key: ByteArray)
    fun retrieveMessageKey(id: String): ByteArray?
    fun deleteMessageKey(id: String)
}