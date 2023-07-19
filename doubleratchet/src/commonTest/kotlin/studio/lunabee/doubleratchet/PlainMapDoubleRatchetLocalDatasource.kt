package studio.lunabee.doubleratchet

import studio.lunabee.doubleratchet.model.AsymmetricKeyPair
import studio.lunabee.doubleratchet.model.ChainKey
import studio.lunabee.doubleratchet.model.Conversation
import studio.lunabee.doubleratchet.model.DoubleRatchetUUID
import studio.lunabee.doubleratchet.model.MessageKey
import studio.lunabee.doubleratchet.model.PrivateKey
import studio.lunabee.doubleratchet.model.PublicKey
import studio.lunabee.doubleratchet.storage.DoubleRatchetLocalDatasource

/**
 * Implementation provided for testing,
 * The data is stored in map and is not encrypted
 */
class PlainMapDoubleRatchetLocalDatasource : DoubleRatchetLocalDatasource {
    val messageKeys: MutableMap<String, MessageKey> = mutableMapOf()
    val chainKeys: MutableMap<DoubleRatchetUUID, ChainKey> = mutableMapOf()
    val conversations: MutableMap<DoubleRatchetUUID, Conversation> = mutableMapOf()

    override suspend fun saveOrUpdateConversation(conversation: Conversation) {
        // deep copy
        conversations[conversation.id] = Conversation.createNew(
            id = conversation.id,
            personalKeyPair = AsymmetricKeyPair(
                PublicKey(conversation.personalKeyPair.publicKey.value.copyOf()),
                PrivateKey(conversation.personalKeyPair.privateKey.value.copyOf()),
            ),
        ).apply {
            sendChainKey = conversation.sendChainKey?.value?.let { ChainKey(it.copyOf()) }
            receiveChainKey = conversation.receiveChainKey?.value?.let { ChainKey(it.copyOf()) }
            contactPublicKey = conversation.contactPublicKey?.value?.let { PublicKey(it.copyOf()) }
            lastMessageReceivedType = conversation.lastMessageReceivedType
            sentLastMessageData = conversation.sentLastMessageData
            receivedLastMessageNumber = conversation.receivedLastMessageNumber
        }
    }

    override suspend fun getConversation(id: DoubleRatchetUUID): Conversation? = conversations[id]

    override suspend fun saveMessageKey(id: String, key: MessageKey) {
        messageKeys[id] = MessageKey(key.value.copyOf())
    }

    override suspend fun saveChainKey(id: DoubleRatchetUUID, key: ChainKey) {
        chainKeys[id] = ChainKey(key.value.copyOf())
    }

    override suspend fun popMessageKey(id: String): MessageKey? {
        return messageKeys.remove(id)
    }

    override suspend fun retrieveChainKey(id: DoubleRatchetUUID): ChainKey? {
        return chainKeys[id]
    }

    override suspend fun deleteChainKey(id: DoubleRatchetUUID) {
        chainKeys.remove(id)
    }
}
