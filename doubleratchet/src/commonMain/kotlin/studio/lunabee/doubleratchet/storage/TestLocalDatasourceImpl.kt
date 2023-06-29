package studio.lunabee.doubleratchet.storage

import studio.lunabee.doubleratchet.model.Conversation

class TestLocalDatasourceImpl : LocalDatasource {
    private val storedKeys: MutableMap<String, ByteArray> = mutableMapOf()
    private val storedConversations: MutableMap<String, Conversation> = mutableMapOf()

    override fun saveOrUpdateConversation(conversation: Conversation) {
        storedConversations[conversation.id] = conversation
    }

    override fun getConversation(id: String): Conversation? = storedConversations[id]

    override fun saveMessageKey(id: String, key: ByteArray) {
        storedKeys[id] = key
    }

    override fun retrieveMessageKey(id: String): ByteArray? = storedKeys[id]

    override fun deleteMessageKey(id: String) {
        storedKeys.remove(id)
    }
}