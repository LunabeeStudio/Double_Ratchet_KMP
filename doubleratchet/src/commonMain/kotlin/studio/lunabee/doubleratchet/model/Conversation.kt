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
    sendChainKey: DRChainKey? = null,
    receiveChainKey: DRChainKey? = null,
    contactPublicKey: DRPublicKey? = null,
    lastMessageReceivedType: MessageType? = null,
    sentLastMessageData: MessageConversationCounter? = null,
    receivedLastMessageNumber: UInt? = null,
) {
    var personalKeyPair: AsymmetricKeyPair = personalKeyPair
        internal set
    var sendChainKey: DRChainKey? = sendChainKey
        internal set
    var receiveChainKey: DRChainKey? = receiveChainKey
        internal set
    var contactPublicKey: DRPublicKey? = contactPublicKey
        internal set
    var lastMessageReceivedType: MessageType? = lastMessageReceivedType
        internal set
    var sentLastMessageData: MessageConversationCounter? = sentLastMessageData
        internal set
    var receivedLastMessageNumber: UInt? = receivedLastMessageNumber
        internal set

    fun isReadyForMessageSending(): Boolean {
        return sendChainKey != null && contactPublicKey != null
    }

    fun isReadyForMessageReceiving(): Boolean {
        return receiveChainKey != null && contactPublicKey != null
    }

    enum class MessageType {
        Sent, Received
    }

    companion object {
        fun createNew(id: DoubleRatchetUUID, personalKeyPair: AsymmetricKeyPair): Conversation = Conversation(
            id = id,
            personalKeyPair = personalKeyPair,
        )

        fun createFromInvitation(
            id: DoubleRatchetUUID,
            personalKeyPair: AsymmetricKeyPair,
            sendChainKey: DRChainKey,
            receiveChainKey: DRChainKey,
            contactPublicKey: DRPublicKey,
        ): Conversation = Conversation(
            id = id,
            personalKeyPair = personalKeyPair,
            sendChainKey = sendChainKey,
            receiveChainKey = receiveChainKey,
            contactPublicKey = contactPublicKey,
        )
    }
}
