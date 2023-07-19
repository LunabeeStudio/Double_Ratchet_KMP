package studio.lunabee.doubleratchet

import studio.lunabee.doubleratchet.model.AsymmetricKeyPair
import studio.lunabee.doubleratchet.model.DRChainKey
import studio.lunabee.doubleratchet.model.Conversation
import studio.lunabee.doubleratchet.model.DoubleRatchetUUID
import studio.lunabee.doubleratchet.model.DRMessageKey
import studio.lunabee.doubleratchet.model.DRPrivateKey
import studio.lunabee.doubleratchet.model.DRPublicKey
import studio.lunabee.doubleratchet.storage.DoubleRatchetLocalDatasource

/**
 * Implementation provided for testing,
 * The data is stored in map and is not encrypted
 */
class PlainMapDoubleRatchetLocalDatasource : DoubleRatchetLocalDatasource {
    val messageKeys: MutableMap<String, DRMessageKey> = mutableMapOf()
    val chainKeys: MutableMap<DoubleRatchetUUID, DRChainKey> = mutableMapOf()
    val conversations: MutableMap<DoubleRatchetUUID, Conversation> = mutableMapOf()

    override suspend fun saveOrUpdateConversation(conversation: Conversation) {
        // deep copy
        conversations[conversation.id] = Conversation.createNew(
            id = conversation.id,
            personalKeyPair = AsymmetricKeyPair(
                DRPublicKey(conversation.personalKeyPair.publicKey.value.copyOf()),
                DRPrivateKey(conversation.personalKeyPair.privateKey.value.copyOf()),
            ),
        ).apply {
            sendChainKey = conversation.sendChainKey?.value?.let { DRChainKey(it.copyOf()) }
            receiveChainKey = conversation.receiveChainKey?.value?.let { DRChainKey(it.copyOf()) }
            contactPublicKey = conversation.contactPublicKey?.value?.let { DRPublicKey(it.copyOf()) }
            lastMessageReceivedType = conversation.lastMessageReceivedType
            sentLastMessageData = conversation.sentLastMessageData
            receivedLastMessageNumber = conversation.receivedLastMessageNumber
        }
    }

    override suspend fun getConversation(id: DoubleRatchetUUID): Conversation? = conversations[id]

    override suspend fun saveMessageKey(id: String, key: DRMessageKey) {
        messageKeys[id] = DRMessageKey(key.value.copyOf())
    }

    override suspend fun saveChainKey(id: DoubleRatchetUUID, key: DRChainKey) {
        chainKeys[id] = DRChainKey(key.value.copyOf())
    }

    override suspend fun popMessageKey(id: String): DRMessageKey? {
        return messageKeys.remove(id)
    }

    override suspend fun retrieveChainKey(id: DoubleRatchetUUID): DRChainKey? {
        return chainKeys[id]
    }

    override suspend fun deleteChainKey(id: DoubleRatchetUUID) {
        chainKeys.remove(id)
    }
}
