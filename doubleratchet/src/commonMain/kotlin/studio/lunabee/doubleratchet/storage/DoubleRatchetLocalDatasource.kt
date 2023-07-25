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

package studio.lunabee.doubleratchet.storage

import studio.lunabee.doubleratchet.model.Conversation
import studio.lunabee.doubleratchet.model.DRMessageKey
import studio.lunabee.doubleratchet.model.DoubleRatchetUUID

/**
 * Used by the doubleRatchet engine to store and retrieve the keys it needs.
 */
interface DoubleRatchetLocalDatasource {
    suspend fun saveOrUpdateConversation(conversation: Conversation)
    suspend fun getConversation(id: DoubleRatchetUUID): Conversation?
    suspend fun saveMessageKey(id: String, key: DRMessageKey)

    /**
     * Return and delete [DRMessageKey] with id [id]
     */
    suspend fun popMessageKey(id: String): DRMessageKey?
}
