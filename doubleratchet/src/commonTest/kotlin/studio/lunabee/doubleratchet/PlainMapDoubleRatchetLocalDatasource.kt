/*
 * Copyright (c) 2023 Lunabee Studio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package studio.lunabee.doubleratchet

import studio.lunabee.doubleratchet.model.AsymmetricKeyPair
import studio.lunabee.doubleratchet.model.Conversation
import studio.lunabee.doubleratchet.model.DRChainKey
import studio.lunabee.doubleratchet.model.DRMessageKey
import studio.lunabee.doubleratchet.model.DRPrivateKey
import studio.lunabee.doubleratchet.model.DRPublicKey
import studio.lunabee.doubleratchet.model.DRRootKey
import studio.lunabee.doubleratchet.model.DoubleRatchetUUID
import studio.lunabee.doubleratchet.storage.DoubleRatchetLocalDatasource

/**
 * Implementation provided for testing,
 * The data is stored in map and is not encrypted
 */
class PlainMapDoubleRatchetLocalDatasource(
    private val id: String,
) : DoubleRatchetLocalDatasource {
    val messageKeys: MutableMap<String, DRMessageKey> = mutableMapOf()
    var conversation: Conversation? = null
        private set(value) {
            check(field == null || value!!.id == field?.id)
            val newRootKey = value?.rootKey
            if (newRootKey != null && !newRootKey.value.contentEquals(rootKeys.lastOrNull()?.value)) {
                rootKeys += DRRootKey(newRootKey.value.copyOf())
            }
            field = value
        }

    val rootKeys: MutableList<DRRootKey> = mutableListOf()

    override suspend fun saveOrUpdateConversation(conversation: Conversation) {
        this.conversation = Conversation(
            id = conversation.id,
            personalKeyPair = AsymmetricKeyPair(
                DRPublicKey(conversation.personalKeyPair.publicKey.value.copyOf()),
                DRPrivateKey(conversation.personalKeyPair.privateKey.value.copyOf()),
            ),
            rootKey = DRRootKey(conversation.rootKey!!.value.copyOf()),
            messageNumber = conversation.nextMessageNumber,
            sequenceNumber = conversation.nextSequenceNumber,
            sendingChainKey = conversation.sendingChainKey?.value?.copyOf()?.let { DRChainKey(it) },
            receiveChainKey = conversation.receiveChainKey?.value?.copyOf()?.let { DRChainKey(it) },
            lastContactPublicKey = conversation.lastContactPublicKey?.value?.copyOf()?.let { DRPublicKey(it) },
            receivedLastMessageNumber = conversation.receivedLastMessageNumber,
        )
    }

    override suspend fun getConversation(id: DoubleRatchetUUID): Conversation? = if (id == conversation?.id) {
        Conversation(
            id = conversation!!.id,
            personalKeyPair = AsymmetricKeyPair(
                DRPublicKey(conversation!!.personalKeyPair.publicKey.value.copyOf()),
                DRPrivateKey(conversation!!.personalKeyPair.privateKey.value.copyOf()),
            ),
            rootKey = DRRootKey(conversation!!.rootKey!!.value.copyOf()),
            messageNumber = conversation!!.nextMessageNumber,
            sequenceNumber = conversation!!.nextSequenceNumber,
            sendingChainKey = conversation!!.sendingChainKey?.value?.copyOf()?.let { DRChainKey(it) },
            receiveChainKey = conversation!!.receiveChainKey?.value?.copyOf()?.let { DRChainKey(it) },
            lastContactPublicKey = conversation!!.lastContactPublicKey?.value?.copyOf()?.let { DRPublicKey(it) },
            receivedLastMessageNumber = conversation!!.receivedLastMessageNumber,
        )
    } else {
        null
    }

    override suspend fun saveMessageKey(id: String, key: DRMessageKey) {
        messageKeys[id] = DRMessageKey(key.value.copyOf())
    }

    override suspend fun popMessageKey(id: String): DRMessageKey? {
        return messageKeys.remove(id)
    }

    override fun toString(): String {
        return "PlainMapDoubleRatchetLocalDatasource(id='$id')"
    }
}
