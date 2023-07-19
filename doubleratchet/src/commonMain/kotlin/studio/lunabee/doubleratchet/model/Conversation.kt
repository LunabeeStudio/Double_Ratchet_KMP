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

data class Conversation(
    val id: DoubleRatchetUUID,
    val sendChainKey: ByteArray? = null,
    val receiveChainKey: ByteArray? = null,
    val contactPublicKey: ByteArray? = null,
    val personalPublicKey: ByteArray,
    val personalPrivateKey: ByteArray,
    val lastMessageReceivedType: MessageType? = null,
    val sentLastMessageData: LastMessageConversationData? = null,
    val receivedLastMessageData: LastMessageConversationData? = null,
) {
    fun isReadyForMessageSending(): Boolean {
        return sendChainKey != null && contactPublicKey != null
    }

    fun isReadyForMessageReceiving(): Boolean {
        return receiveChainKey != null && contactPublicKey != null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Conversation) return false

        if (id != other.id) return false
        if (sendChainKey != null) {
            if (other.sendChainKey == null) return false
            if (!sendChainKey.contentEquals(other.sendChainKey)) return false
        } else if (other.sendChainKey != null) return false
        if (receiveChainKey != null) {
            if (other.receiveChainKey == null) return false
            if (!receiveChainKey.contentEquals(other.receiveChainKey)) return false
        } else if (other.receiveChainKey != null) return false
        if (contactPublicKey != null) {
            if (other.contactPublicKey == null) return false
            if (!contactPublicKey.contentEquals(other.contactPublicKey)) return false
        } else if (other.contactPublicKey != null) return false
        if (!personalPublicKey.contentEquals(other.personalPublicKey)) return false
        if (!personalPrivateKey.contentEquals(other.personalPrivateKey)) return false
        if (lastMessageReceivedType != other.lastMessageReceivedType) return false
        if (sentLastMessageData != other.sentLastMessageData) return false
        if (receivedLastMessageData != other.receivedLastMessageData) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (sendChainKey?.contentHashCode() ?: 0)
        result = 31 * result + (receiveChainKey?.contentHashCode() ?: 0)
        result = 31 * result + (contactPublicKey?.contentHashCode() ?: 0)
        result = 31 * result + personalPublicKey.contentHashCode()
        result = 31 * result + personalPrivateKey.contentHashCode()
        result = 31 * result + (lastMessageReceivedType?.hashCode() ?: 0)
        result = 31 * result + (sentLastMessageData?.hashCode() ?: 0)
        result = 31 * result + (receivedLastMessageData?.hashCode() ?: 0)
        return result
    }

    enum class MessageType {
        Sent, Received
    }
}

class LastMessageConversationData(
    val messageNumber: Int,
    val sequenceNumber: Int,
)