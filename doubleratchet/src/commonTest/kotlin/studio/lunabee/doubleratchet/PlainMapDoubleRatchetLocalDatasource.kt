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
    val chainKeys: MutableMap<String, ChainKey> = mutableMapOf()
    val conversations: MutableMap<DoubleRatchetUUID, Conversation> = mutableMapOf()

    override suspend fun saveOrUpdateConversation(conversation: Conversation) {
        // deep copy
        conversations[conversation.id] = Conversation(
            conversation.id,
            AsymmetricKeyPair(
                PublicKey(conversation.personalKeyPair.publicKey.value.copyOf()),
                PrivateKey(conversation.personalKeyPair.privateKey.value.copyOf()),
            ),
            conversation.sendChainKey?.value?.let { ChainKey(it.copyOf()) },
            conversation.receiveChainKey?.value?.let { ChainKey(it.copyOf()) },
            conversation.contactPublicKey?.value?.let { PublicKey(it.copyOf()) },
            conversation.lastMessageReceivedType,
            conversation.sentLastMessageData,
            conversation.receivedLastMessageData,
        )
    }

    override suspend fun getConversation(id: DoubleRatchetUUID): Conversation? = conversations[id]

    override suspend fun saveMessageKey(id: String, key: MessageKey) {
        messageKeys[id] = MessageKey(key.value.copyOf())
    }

    override suspend fun saveChainKey(id: String, key: ChainKey) {
        chainKeys[id] = ChainKey(key.value.copyOf())
    }

    override suspend fun popMessageKey(id: String): MessageKey? {
        return messageKeys.remove(id)
    }

    override suspend fun retrieveChainKey(id: String): ChainKey? {
        return chainKeys[id]
    }

    override suspend fun deleteChainKey(id: String) {
        chainKeys.remove(id)
    }
}
