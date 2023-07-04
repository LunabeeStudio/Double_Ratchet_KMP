package studio.lunabee.doubleratchet

import studio.lunabee.doubleratchet.model.Conversation
import studio.lunabee.doubleratchet.storage.DoubleRatchetLocalDatasource

class TestDoubleRatchetLocalDatasourceImpl : DoubleRatchetLocalDatasource {
    private val storedKeys: MutableMap<String, ByteArray> = mutableMapOf()
    private val storedConversations: MutableMap<String, Conversation> = mutableMapOf()

    override suspend fun saveOrUpdateConversation(conversation: Conversation) {
        storedConversations[conversation.id] = conversation
    }

    override suspend fun getConversation(id: String): Conversation? = storedConversations[id]

    override suspend fun saveMessageKey(id: String, key: ByteArray) {
        storedKeys[id] = key
    }

    override suspend fun retrieveMessageKey(id: String): ByteArray? = storedKeys[id]

    override suspend fun deleteMessageKey(id: String) {
        storedKeys.remove(id)
    }
}