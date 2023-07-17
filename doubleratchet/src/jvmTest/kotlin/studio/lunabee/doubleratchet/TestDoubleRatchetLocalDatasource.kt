package studio.lunabee.doubleratchet

import studio.lunabee.doubleratchet.model.Conversation
import studio.lunabee.doubleratchet.model.DoubleRatchetUUID
import studio.lunabee.doubleratchet.storage.DoubleRatchetLocalDatasource

/**
 * Implementation provided for testing,
 * The data is stored in map and is not encrypted
 */
actual class TestDoubleRatchetLocalDatasource : DoubleRatchetLocalDatasource {
    private val storedKeys: MutableMap<String, ByteArray> = mutableMapOf()
    private val storedConversations: MutableMap<DoubleRatchetUUID, Conversation> = mutableMapOf()

    override suspend fun saveOrUpdateConversation(conversation: Conversation) {
        storedConversations[conversation.id] = conversation
    }

    override suspend fun getConversation(id: DoubleRatchetUUID): Conversation? = storedConversations[id]

    override suspend fun saveMessageKey(id: String, key: ByteArray) {
        storedKeys[id] = key
    }

    override suspend fun retrieveMessageKey(id: String): ByteArray? = storedKeys[id]

    override suspend fun deleteMessageKey(id: String) {
        storedKeys.remove(id)
    }
}