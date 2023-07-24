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

package studio.lunabee.doubleratchet.model

class Conversation private constructor(
    val id: DoubleRatchetUUID,
    personalKeyPair: AsymmetricKeyPair,
    messageNumber: UInt = 0u,
    sequenceNumber: UInt = 0u,
    rootKey: DRRootKey? = null,
    sendingChainKey: DRChainKey? = null,
    receiveChainKey: DRChainKey? = null,
    lastContactPublicKey: DRPublicKey? = null,
    receivedLastMessageNumber: UInt? = null,
) {
    var personalKeyPair: AsymmetricKeyPair = personalKeyPair
        internal set
    var rootKey: DRRootKey? = rootKey
        internal set
    var sendingChainKey: DRChainKey? = sendingChainKey
        internal set
    var receiveChainKey: DRChainKey? = receiveChainKey
        internal set
    var lastContactPublicKey: DRPublicKey? = lastContactPublicKey
        internal set
    var nextMessageNumber: UInt = messageNumber
        internal set
    var nextSequenceNumber: UInt = sequenceNumber
        internal set
    var receivedLastMessageNumber: UInt? = receivedLastMessageNumber
        internal set

    fun destroy() {
        rootKey?.destroy()
        sendingChainKey?.destroy()
        receiveChainKey?.destroy()
        lastContactPublicKey?.destroy()
    }

    companion object {
        fun createNew(id: DoubleRatchetUUID, personalKeyPair: AsymmetricKeyPair): Conversation = Conversation(
            id = id,
            personalKeyPair = personalKeyPair,
        )

        fun createFromInvitation(
            id: DoubleRatchetUUID,
            personalKeyPair: AsymmetricKeyPair,
            rootKey: DRRootKey,
            sendingChainKey: DRChainKey,
        ): Conversation = Conversation(
            id = id,
            personalKeyPair = personalKeyPair,
            rootKey = rootKey,
            sendingChainKey = sendingChainKey,
        )
    }
}
