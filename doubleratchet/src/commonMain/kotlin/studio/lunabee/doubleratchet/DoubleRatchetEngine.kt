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

import studio.lunabee.doubleratchet.crypto.DoubleRatchetKeyRepository
import studio.lunabee.doubleratchet.model.AsymmetricKeyPair
import studio.lunabee.doubleratchet.model.Conversation
import studio.lunabee.doubleratchet.model.DRMessageKey
import studio.lunabee.doubleratchet.model.DRPublicKey
import studio.lunabee.doubleratchet.model.DRRootKey
import studio.lunabee.doubleratchet.model.DRSharedSecret
import studio.lunabee.doubleratchet.model.DerivedKeyMessagePair
import studio.lunabee.doubleratchet.model.DoubleRatchetError
import studio.lunabee.doubleratchet.model.DoubleRatchetUUID
import studio.lunabee.doubleratchet.model.InvitationData
import studio.lunabee.doubleratchet.model.MessageConversationCounter
import studio.lunabee.doubleratchet.model.MessageHeader
import studio.lunabee.doubleratchet.model.SendMessageData
import studio.lunabee.doubleratchet.model.createRandomUUID
import studio.lunabee.doubleratchet.model.use
import studio.lunabee.doubleratchet.storage.DoubleRatchetLocalDatasource

class DoubleRatchetEngine(
    private val doubleRatchetLocalDatasource: DoubleRatchetLocalDatasource,
    private val doubleRatchetKeyRepository: DoubleRatchetKeyRepository,
) {

    /**
     * Generate the data needed to create an invitation to start a new conversation
     *
     * @param newConversationId The id to be used for the conversation
     *
     * @return the conversation id
     */
    suspend fun createInvitation(newConversationId: DoubleRatchetUUID = createRandomUUID()): InvitationData {
        val keyPair: AsymmetricKeyPair = doubleRatchetKeyRepository.generateKeyPair()
        val conversation = Conversation.createNew(
            id = newConversationId,
            personalKeyPair = keyPair,
        )
        doubleRatchetLocalDatasource.saveOrUpdateConversation(conversation)
        keyPair.privateKey.destroy()
        return InvitationData(
            conversationId = newConversationId,
            publicKey = keyPair.publicKey,
        )
    }

    /**
     * Generate the data needed to start a new conversation from an invitation
     *
     * @param contactPublicKey The public key coming from the invitation
     * @param sharedSalt An initial shared salt. Could be a constant.
     * @param newConversationId The id to be used for the conversation
     *
     * @return the conversation id
     */
    suspend fun createNewConversationFromInvitation(
        contactPublicKey: DRPublicKey,
        sharedSalt: DRSharedSecret,
        newConversationId: DoubleRatchetUUID = createRandomUUID(),
    ): DoubleRatchetUUID {
        val keyPair: AsymmetricKeyPair = doubleRatchetKeyRepository.generateKeyPair()
        val sharedSecret = doubleRatchetKeyRepository.createDiffieHellmanSharedSecret(contactPublicKey, keyPair.privateKey)
        val rootKey = DRRootKey(sharedSalt.value) // Initial root key is the shared secret

        val keyRootPair = doubleRatchetKeyRepository.deriveRootKeys(rootKey, sharedSecret)

        val conversation = Conversation.createFromInvitation(
            id = newConversationId,
            personalKeyPair = keyPair,
            rootKey = keyRootPair.rootKey,
            sendingChainKey = keyRootPair.chainKey,
        )

        try {
            doubleRatchetLocalDatasource.saveOrUpdateConversation(conversation)
        } finally {
            keyRootPair.rootKey.destroy()
            keyRootPair.chainKey.destroy()
            keyPair.privateKey.destroy()
            keyPair.publicKey.destroy()
            contactPublicKey.destroy()
        }

        return newConversationId
    }

    /**
     * Generate new message header to attach to the message you want to send, and the message key to use to encrypt the message
     */
    suspend fun getSendData(conversationId: DoubleRatchetUUID): SendMessageData {
        val conversation = doubleRatchetLocalDatasource.getConversation(conversationId)
            ?: throw DoubleRatchetError(DoubleRatchetError.Type.ConversationNotFound)
        if (!conversation.isReadyForMessageSending()) throw DoubleRatchetError(DoubleRatchetError.Type.ConversationNotSetup)

        val sendingChainKey = conversation.sendingChainKey!!
        val derivedKeyPair = doubleRatchetKeyRepository.deriveChainKeys(sendingChainKey)

        val messageData = SendMessageData(
            messageHeader = MessageHeader(
                counter = MessageConversationCounter(
                    message = conversation.nextSendMessageData.message,
                    sequence = conversation.nextSendMessageData.sequence,
                ),
                publicKey = conversation.personalKeyPair.publicKey,
            ),
            messageKey = derivedKeyPair.messageKey,
        )

        conversation.apply {
            this.sendingChainKey = derivedKeyPair.chainKey // TODO optim already updated by deriveChainKeys
            this.nextSendMessageData = MessageConversationCounter(
                message = conversation.nextSendMessageData.message.inc(),
                sequence = conversation.nextSendMessageData.sequence.inc(),
            )
        }
        doubleRatchetLocalDatasource.saveOrUpdateConversation(conversation)
        derivedKeyPair.chainKey.destroy()

        return messageData
    }

    /**
     * @return the key needed to decrypt the message attached to the messageHeader
     */
    suspend fun getReceiveKey(
        messageHeader: MessageHeader,
        conversationId: DoubleRatchetUUID,
        sharedSalt: DRSharedSecret? = null, // TODO move to repo ?
    ): DRMessageKey {
        val conversation = doubleRatchetLocalDatasource.getConversation(conversationId)
            ?: throw DoubleRatchetError(DoubleRatchetError.Type.ConversationNotFound)

        val isMessageKeyAlreadyGenerated = conversation.receivedLastMessageNumber?.let { messageCount ->
            messageCount >= messageHeader.counter.message
        } ?: false

        return if (isMessageKeyAlreadyGenerated) {
            popStoredMessageKey(messageHeader, conversationId)
        } else {
            if (conversation.rootKey == null) {
                conversation.apply {
                    this.rootKey = DRRootKey(sharedSalt!!.value) // Initial root key is the shared secret
                }
            }

            computeNextMessageKey(messageHeader, conversation)
        }
    }

    private suspend fun popStoredMessageKey(
        messageHeader: MessageHeader,
        conversationId: DoubleRatchetUUID,
    ): DRMessageKey {
        val messageKeyId = getMessageKeyId(conversationId, messageHeader.counter.message)
        return doubleRatchetLocalDatasource.popMessageKey(messageKeyId)
            ?: throw DoubleRatchetError(DoubleRatchetError.Type.MessageKeyNotFound)
    }

    private suspend fun computeNextMessageKey(
        messageHeader: MessageHeader,
        conversation: Conversation,
    ): DRMessageKey {
        var workingConversation = conversation
        val lastMessageNumber = workingConversation.receivedLastMessageNumber

        val newSequenceMessageNumber: UInt? = if (messageHeader.publicKey.contentEquals(workingConversation.lastContactPublicKey)) {
            null
        } else {
            messageHeader.counter.message - messageHeader.counter.sequence
        }

        var messageNumber = lastMessageNumber?.inc() ?: 0u
        var messageKey: DRMessageKey? = null
        val sharedSecret = lazy { DRSharedSecret.empty() }
        val derivedKeyPair = DerivedKeyMessagePair.empty()
        while (messageKey == null) {
            if (messageNumber == newSequenceMessageNumber) {
                sharedSecret.value.use {
                    receiveNewSequenceMessage(messageHeader.publicKey, workingConversation, sharedSecret.value, derivedKeyPair)
                }
                // Update for next sending
                updateConversationForNextSend(messageHeader, workingConversation)
            } else {
                receiveOldSequenceMessage(workingConversation, derivedKeyPair)
            }
            workingConversation = doubleRatchetLocalDatasource.getConversation(conversation.id)
                ?: throw DoubleRatchetError(DoubleRatchetError.Type.ConversationNotFound)

            if (messageNumber == messageHeader.counter.message) {
                messageKey = derivedKeyPair.messageKey
            } else {
                doubleRatchetLocalDatasource.saveMessageKey(
                    id = getMessageKeyId(conversation.id, messageNumber),
                    key = derivedKeyPair.messageKey,
                )
                derivedKeyPair.messageKey.destroy()
                messageNumber++
            }
        }

        return messageKey
    }

    private suspend fun receiveOldSequenceMessage(
        conversation: Conversation,
        derivedKeyPair: DerivedKeyMessagePair,
    ) {
        doubleRatchetKeyRepository.deriveChainKeys(conversation.receiveChainKey!!, derivedKeyPair)
        conversation.apply {
            receiveChainKey = derivedKeyPair.chainKey
            receivedLastMessageNumber = conversation.receivedLastMessageNumber?.inc() ?: 1u
        }
        doubleRatchetLocalDatasource.saveOrUpdateConversation(conversation)
        derivedKeyPair.chainKey.destroy()
    }

    private suspend fun receiveNewSequenceMessage(
        publicKey: DRPublicKey,
        conversation: Conversation,
        sharedSecret: DRSharedSecret,
        derivedKeyPair: DerivedKeyMessagePair,
    ) {
        doubleRatchetKeyRepository.createDiffieHellmanSharedSecret(
            publicKey = publicKey,
            privateKey = conversation.personalKeyPair.privateKey,
            out = sharedSecret,
        )
        // TODO optim byteArray newDerivedKeyRootPair.chainKey (?)
        val newDerivedKeyRootPair = doubleRatchetKeyRepository.deriveRootKeys(
            rootKey = conversation.rootKey!!,
            sharedSecret = sharedSecret,
        )
        val newDerivedKeyPair = doubleRatchetKeyRepository.deriveChainKeys(
            chainKey = newDerivedKeyRootPair.chainKey,
            out = derivedKeyPair,
        )

        sharedSecret.destroy()
        conversation.apply {
            this.rootKey = newDerivedKeyRootPair.rootKey
            this.receiveChainKey = newDerivedKeyPair.chainKey
            this.lastContactPublicKey = publicKey
            this.receivedLastMessageNumber = conversation.receivedLastMessageNumber?.inc() ?: 0u
        }
        doubleRatchetLocalDatasource.saveOrUpdateConversation(conversation)
    }

    private suspend fun updateConversationForNextSend(
        messageHeader: MessageHeader,
        conversation: Conversation,
    ) {
        val newKeyPair = doubleRatchetKeyRepository.generateKeyPair()
        val sharedSecret = doubleRatchetKeyRepository.createDiffieHellmanSharedSecret(
            messageHeader.publicKey,
            newKeyPair.privateKey,
        )
        val derivedKeyPair = doubleRatchetKeyRepository.deriveRootKeys(conversation.rootKey!!, sharedSecret)
        sharedSecret.destroy()

        conversation.apply {
            this.sendingChainKey = derivedKeyPair.chainKey
            this.rootKey = derivedKeyPair.rootKey
            this.personalKeyPair = newKeyPair
            this.nextSendMessageData = MessageConversationCounter(
                message = conversation.nextSendMessageData.message,
                sequence = 0u,
            )
        }

        doubleRatchetLocalDatasource.saveOrUpdateConversation(conversation)
        derivedKeyPair.chainKey.destroy()
        derivedKeyPair.rootKey.destroy()
        newKeyPair.privateKey.destroy()
        newKeyPair.publicKey.destroy()
    }

    private fun getMessageKeyId(conversationId: DoubleRatchetUUID, messageNumber: UInt): String {
        return "${conversationId.uuidString()} - $messageNumber"
    }
}
