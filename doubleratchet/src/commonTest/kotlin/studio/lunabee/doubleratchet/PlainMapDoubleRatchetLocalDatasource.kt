package studio.lunabee.doubleratchet

import studio.lunabee.doubleratchet.model.ChainKey
import studio.lunabee.doubleratchet.model.Conversation
import studio.lunabee.doubleratchet.model.DoubleRatchetUUID
import studio.lunabee.doubleratchet.model.MessageKey
import studio.lunabee.doubleratchet.storage.DoubleRatchetLocalDatasource

/**
 * Implementation provided for testing,
 * The data is stored in map and is not encrypted
 */
class PlainMapDoubleRatchetLocalDatasource : DoubleRatchetLocalDatasource {
    private val storedKeys: MutableMap<String, ByteArray> = mutableMapOf()
    private val storedConversations: MutableMap<DoubleRatchetUUID, Conversation> = mutableMapOf()

    override suspend fun saveOrUpdateConversation(conversation: Conversation) {
        storedConversations[conversation.id] = conversation
    }

    override suspend fun getConversation(id: DoubleRatchetUUID): Conversation? = storedConversations[id]

    override suspend fun saveMessageKey(id: String, key: MessageKey) {
        saveKey(id, key.value)
    }

    override suspend fun saveChainKey(id: String, key: ChainKey) {
        saveKey(id, key.value)
    }

    override suspend fun retrieveMessageKey(id: String): MessageKey? {
        return retrieveKey(id)?.let { MessageKey(it) }
    }

    override suspend fun retrieveChainKey(id: String): ChainKey? {
        return retrieveKey(id)?.let { ChainKey(it) }
    }

    private fun saveKey(id: String, key: ByteArray) {
        storedKeys[id] = key
    }

    private fun retrieveKey(id: String): ByteArray? = storedKeys[id]

    override suspend fun deleteMessageKey(id: String) {
        storedKeys.remove(id)
    }
}
