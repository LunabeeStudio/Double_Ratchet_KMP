package studio.lunabee.doubleratchet

import studio.lunabee.doubleratchet.model.Conversation
import studio.lunabee.doubleratchet.model.DoubleRatchetUUID
import studio.lunabee.doubleratchet.storage.DoubleRatchetLocalDatasource

actual class TestDoubleRatchetLocalDatasource : DoubleRatchetLocalDatasource {
    override suspend fun saveOrUpdateConversation(conversation: Conversation) {
        TODO("Not yet implemented")
    }

    override suspend fun getConversation(id: DoubleRatchetUUID): Conversation? {
        TODO("Not yet implemented")
    }

    override suspend fun saveMessageKey(id: String, key: ByteArray) {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveMessageKey(id: String): ByteArray? {
        TODO("Not yet implemented")
    }

    override suspend fun deleteMessageKey(id: String) {
        TODO("Not yet implemented")
    }

}

