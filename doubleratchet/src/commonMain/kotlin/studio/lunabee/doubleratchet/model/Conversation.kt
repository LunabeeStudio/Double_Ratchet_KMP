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

/**
 * State of a conversation containing states variables required to run the double ratchet algorithm from each party.
 *
 * @property id The unique and local identifier of a conversation
 * @property personalKeyPair The sending key pair, formally DHs
 * @property rootKey The current root key, formally RK
 * @property sendingChainKey The current chain key for sending, formally CKs
 * @property receiveChainKey The current chain key for receiving, formally CKr
 * @property lastContactPublicKey The last known public key of the emitter, formally DHr
 * @property nextMessageNumber The number of the next message to be sent, formally Ns+1
 * @property nextSequenceNumber Number of messages in the previous sequence of messages, formally PN
 * @property receivedLastMessageNumber The number of the last message received. Used to know if some messages have been skipped.
 */
class Conversation(
    val id: DoubleRatchetUUID,
    personalKeyPair: AsymmetricKeyPair,
    messageNumber: Int = 0,
    sequenceNumber: Int = 0,
    rootKey: DRRootKey? = null,
    sendingChainKey: DRChainKey? = null,
    receiveChainKey: DRChainKey? = null,
    lastContactPublicKey: DRPublicKey? = null,
    receivedLastMessageNumber: Int? = null,
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
    var nextMessageNumber: Int = messageNumber
        internal set
    var nextSequenceNumber: Int = sequenceNumber
        internal set
    var receivedLastMessageNumber: Int? = receivedLastMessageNumber
        internal set

    /**
     * Erase sensitive in-memory data
     */
    fun destroy() {
        personalKeyPair.privateKey.destroy()
        rootKey?.destroy()
        sendingChainKey?.destroy()
        receiveChainKey?.destroy()
        lastContactPublicKey?.destroy()
    }

    companion object {
        fun createNew(id: DoubleRatchetUUID, personalKeyPair: AsymmetricKeyPair, initialRootKey: DRRootKey): Conversation = Conversation(
            id = id,
            personalKeyPair = personalKeyPair,
            rootKey = initialRootKey,
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
