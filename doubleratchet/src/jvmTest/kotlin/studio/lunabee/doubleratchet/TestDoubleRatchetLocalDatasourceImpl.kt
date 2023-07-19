/*
 * Copyright (c) 2023-2023 Lunabee Studio
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

import studio.lunabee.doubleratchet.model.Conversation
import studio.lunabee.doubleratchet.model.DoubleRatchetUUID
import studio.lunabee.doubleratchet.storage.DoubleRatchetLocalDatasource

/**
 * Implementation provided for testing,
 * The data is stored in map and is not encrypted
 */
class TestDoubleRatchetLocalDatasourceImpl : DoubleRatchetLocalDatasource {
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