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

import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import studio.lunabee.doubleratchet.model.DRChainKey
import studio.lunabee.doubleratchet.model.DRRootKey
import studio.lunabee.doubleratchet.model.DRSharedSecret
import studio.lunabee.doubleratchet.model.DoubleRatchetError
import studio.lunabee.doubleratchet.model.DoubleRatchetUUID
import studio.lunabee.doubleratchet.model.InvitationData
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ConversationTest {

    companion object {
        private val random: Random = RandomProviderTest.random
        private val keyLength: Int = random.nextInt(20, 80)
    }

    /**
     * @see <a href="https://signal.org/docs/specifications/doubleratchet/#diffie-hellman-ratchet">
     *     Diffie-Hellman ratchet</a>
     */
    @Test
    fun `DH shared secret algorithm test`(): TestResult = runTest {
        val cryptoRepositoryA = getRepository()
        val cryptoRepositoryB = getRepository()
        val keyPairAlice = cryptoRepositoryA.generateKeyPair()
        val keyPairBob = cryptoRepositoryB.generateKeyPair()
        val sharedSecretAlice = DRSharedSecret(ByteArray(cryptoRepositoryA.sharedSecretByteSize) { -1 })
        val sharedSecretBob = DRSharedSecret(ByteArray(cryptoRepositoryB.sharedSecretByteSize) { -2 })

        cryptoRepositoryA.createDiffieHellmanSharedSecret(keyPairBob.publicKey, keyPairAlice.privateKey, sharedSecretAlice)
        cryptoRepositoryB.createDiffieHellmanSharedSecret(keyPairAlice.publicKey, keyPairBob.privateKey, sharedSecretBob)

        assertContentEquals(sharedSecretAlice.value, sharedSecretBob.value)
    }

    /**
     * @see <a href="https://signal.org/docs/specifications/doubleratchet/#kdf-chains">
     *     KDF chains</a>
     */
    @Test
    fun `KDF Root algorithm test`(): TestResult = runTest {
        val sharedSecret = DRSharedSecret(random.nextBytes(keyLength))
        val rootKey = DRRootKey(random.nextBytes(keyLength))
        val cryptoRepository1 = getRepository()
        val cryptoRepository2 = getRepository()
        val rootKey2 = DRRootKey(rootKey.value.copyOf())
        val value1 = cryptoRepository1.deriveRootKeys(rootKey, sharedSecret)
        val value2 = cryptoRepository2.deriveRootKeys(rootKey2, sharedSecret)

        assertContentEquals(value1.rootKey.value, value2.rootKey.value)
        assertContentEquals(value1.chainKey.value, value2.chainKey.value)
    }

    /**
     * @see <a href="https://signal.org/docs/specifications/doubleratchet/#kdf-chains">
     *     KDF chains</a>
     */
    @Test
    fun `KDF Chain algorithm test`(): TestResult = runTest {
        val chainKey = DRChainKey(random.nextBytes(keyLength))
        val chainKey2 = DRChainKey(chainKey.value.copyOf())
        val cryptoRepository1 = getRepository()
        val cryptoRepository2 = getRepository()
        val value1 = cryptoRepository1.deriveChainKeys(chainKey)
        val value2 = cryptoRepository2.deriveChainKeys(chainKey2)

        assertContentEquals(value1.messageKey.value, value2.messageKey.value)
        assertContentEquals(value1.chainKey.value, value2.chainKey.value)
    }

    /**
     * @see <a href="https://signal.org/docs/specifications/doubleratchet/#double-ratchet">
     *     Double Ratchet</a>
     */
    @Test
    fun `Double Ratchet algorithm test`(): TestResult = runTest {
        val sharedSecret = DRSharedSecret(random.nextBytes(keyLength))
        val rootKey = DRRootKey(random.nextBytes(keyLength))
        val rootKey2 = DRRootKey(rootKey.value.copyOf())
        val cryptoRepository1 = getRepository()
        val cryptoRepository2 = getRepository()
        val chainKey = DRChainKey.empty(keyLength)
        val chainKey2 = DRChainKey.empty(keyLength)

        cryptoRepository1.deriveRootKeys(rootKey, sharedSecret, rootKey, chainKey)
        val value1 = cryptoRepository1.deriveChainKeys(chainKey)

        cryptoRepository2.deriveRootKeys(rootKey2, sharedSecret, rootKey, chainKey2)
        val value2 = cryptoRepository2.deriveChainKeys(chainKey2)

        assertContentEquals(value1.messageKey.value, value2.messageKey.value)
        assertContentEquals(value1.chainKey.value, value2.chainKey.value)
    }

    @Test
    fun `Run conversation not setup test`(): TestResult = runTest {
        val engineBob = DoubleRatchetEngine(
            doubleRatchetLocalDatasource = PlainMapDoubleRatchetLocalDatasource("B"),
            doubleRatchetKeyRepository = getRepository(),
        )
        val bobToAliceInvitation: InvitationData = engineBob.createInvitation()
        val error = assertFailsWith(DoubleRatchetError::class) {
            engineBob.getSendData(conversationId = bobToAliceInvitation.conversationId)
        }
        assertEquals(DoubleRatchetError.Type.ConversationNotSetup, error.type)
    }

    @Test
    fun `Initial root key sharing test`(): TestResult = runTest {
        val sharedInit = DRSharedSecret(random.nextBytes(keyLength))
        val datasourceA = PlainMapDoubleRatchetLocalDatasource("A")
        val datasourceB = PlainMapDoubleRatchetLocalDatasource("B")
        val engineA = DoubleRatchetEngine(
            doubleRatchetLocalDatasource = datasourceA,
            doubleRatchetKeyRepository = getRepository(),
        )
        val engineB = DoubleRatchetEngine(
            doubleRatchetLocalDatasource = datasourceB,
            doubleRatchetKeyRepository = getRepository(),
        )

        val invitationFromA = engineA.createInvitation()
        val conversationIdA = invitationFromA.conversationId
        val conversationIdB = engineB.createNewConversationFromInvitation(invitationFromA.publicKey, sharedInit)
        val headerB = engineB.getSendData(conversationIdB)
        engineA.getFirstReceiveKey(headerB.messageHeader, conversationIdA, sharedInit)

        val valuesB = datasourceB.rootKeys.map { key -> key.value.contentHashCode() }
        val valuesA = datasourceA.rootKeys.map { key -> key.value.contentHashCode() }
        assertTrue(valuesA.none { value -> value in valuesB })
    }

    @Test
    fun `Decrypt already decrypted message failure test`(): TestResult = runTest {
        val sharedInit = DRSharedSecret(random.nextBytes(keyLength))
        val engineAlice = DoubleRatchetEngine(
            doubleRatchetLocalDatasource = PlainMapDoubleRatchetLocalDatasource("A"),
            doubleRatchetKeyRepository = getRepository(),
        )
        val engineBob = DoubleRatchetEngine(
            doubleRatchetLocalDatasource = PlainMapDoubleRatchetLocalDatasource("B"),
            doubleRatchetKeyRepository = getRepository(),
        )
        val bobToAliceInvitation: InvitationData = engineBob.createInvitation()
        val aliceToBobConversationId: DoubleRatchetUUID =
            engineAlice.createNewConversationFromInvitation(bobToAliceInvitation.publicKey, sharedInit)
        val aliceMessage1 = engineAlice.getSendData(conversationId = aliceToBobConversationId)
        val receivedBob1 = engineBob.getFirstReceiveKey(aliceMessage1.messageHeader, bobToAliceInvitation.conversationId, sharedInit)
        assertContentEquals(aliceMessage1.messageKey.value, receivedBob1.value)
        val receivedBis = assertFailsWith(DoubleRatchetError::class) {
            engineBob.getReceiveKey(aliceMessage1.messageHeader, bobToAliceInvitation.conversationId)
        }
        assertEquals(DoubleRatchetError.Type.MessageKeyNotFound, receivedBis.type)
    }

    @Test
    fun `Run handshake flow test`(): TestResult = runTest {
        val sharedInit = DRSharedSecret(random.nextBytes(keyLength))
        val engineAlice = DoubleRatchetEngine(
            doubleRatchetLocalDatasource = PlainMapDoubleRatchetLocalDatasource("A"),
            doubleRatchetKeyRepository = getRepository(),
        )
        val engineBob = DoubleRatchetEngine(
            doubleRatchetLocalDatasource = PlainMapDoubleRatchetLocalDatasource("B"),
            doubleRatchetKeyRepository = getRepository(),
        )
        val bobToAliceInvitation: InvitationData = engineBob.createInvitation()
        val aliceToBobConversationId: DoubleRatchetUUID =
            engineAlice.createNewConversationFromInvitation(bobToAliceInvitation.publicKey, sharedInit)

        val aliceMessage1 = engineAlice.getSendData(conversationId = aliceToBobConversationId)
        val receivedBob1 = engineBob.getFirstReceiveKey(aliceMessage1.messageHeader, bobToAliceInvitation.conversationId, sharedInit)
        assertContentEquals(aliceMessage1.messageKey.value, receivedBob1.value)
        val aliceMessage2 = engineAlice.getSendData(conversationId = aliceToBobConversationId)
        val receivedBob2 = engineBob.getReceiveKey(aliceMessage2.messageHeader, bobToAliceInvitation.conversationId)
        assertContentEquals(aliceMessage2.messageKey.value, receivedBob2.value)
        val aliceMessage3 = engineAlice.getSendData(conversationId = aliceToBobConversationId)
        val receivedBob3 = engineBob.getReceiveKey(aliceMessage3.messageHeader, bobToAliceInvitation.conversationId)
        assertContentEquals(aliceMessage3.messageKey.value, receivedBob3.value)
        val bobMessage1 = engineBob.getSendData(bobToAliceInvitation.conversationId)
        val receiveAlice1 = engineAlice.getReceiveKey(bobMessage1.messageHeader, aliceToBobConversationId)
        assertContentEquals(bobMessage1.messageKey.value, receiveAlice1.value)
        val bobMessage2 = engineBob.getSendData(bobToAliceInvitation.conversationId)
        val receiveAlice2 = engineAlice.getReceiveKey(bobMessage2.messageHeader, aliceToBobConversationId)
        assertContentEquals(bobMessage2.messageKey.value, receiveAlice2.value)
    }

    /**
     * @see <a href="https://signal.org/docs/specifications/doubleratchet/#out-of-order-messages">
     *     Out-of-order messages</a>
     */
    @Test
    fun `Run basic out-of-order case test`(): TestResult = runTest {
        val sharedInit = DRSharedSecret(random.nextBytes(keyLength))
        val datasourceA = PlainMapDoubleRatchetLocalDatasource("A")
        val engineAlice = DoubleRatchetEngine(
            doubleRatchetLocalDatasource = datasourceA,
            doubleRatchetKeyRepository = getRepository(),
        )
        val datasourceB = PlainMapDoubleRatchetLocalDatasource("B")
        val engineBob = DoubleRatchetEngine(
            doubleRatchetLocalDatasource = datasourceB,
            doubleRatchetKeyRepository = getRepository(),
        )

        // Bob invite Alice
        val bobToAliceInvitation: InvitationData = engineBob.createInvitation()

        // Alice Accept Bob invitation
        val aliceToBobConversationId: DoubleRatchetUUID =
            engineAlice.createNewConversationFromInvitation(bobToAliceInvitation.publicKey, sharedInit)

        // Alice send a message to Bob
        val aliceMessage1 = engineAlice.getSendData(conversationId = aliceToBobConversationId)

        // Bob receives the message
        val receivedBob1 = engineBob.getFirstReceiveKey(aliceMessage1.messageHeader, bobToAliceInvitation.conversationId, sharedInit)
        assertContentEquals(aliceMessage1.messageKey.value, receivedBob1.value)

        // Alice sends a message to bob again, but bob misses it
        val aliceMessage2 = engineAlice.getSendData(conversationId = aliceToBobConversationId) // Missed
        val aliceMessage3 = engineAlice.getSendData(conversationId = aliceToBobConversationId) // Missed
        val aliceMessage4 = engineAlice.getSendData(conversationId = aliceToBobConversationId) // RECEIVED
        val receivedBob4 = engineBob.getReceiveKey(aliceMessage4.messageHeader, bobToAliceInvitation.conversationId)
        assertContentEquals(aliceMessage4.messageKey.value, receivedBob4.value)
        val receivedBob3 = engineBob.getReceiveKey(aliceMessage3.messageHeader, bobToAliceInvitation.conversationId)
        val receivedBob2 = engineBob.getReceiveKey(
            aliceMessage2.messageHeader,
            bobToAliceInvitation.conversationId,
        ) // Finally Received
        assertContentEquals(aliceMessage2.messageKey.value, receivedBob2.value)
        assertContentEquals(aliceMessage3.messageKey.value, receivedBob3.value)

        assertIsEmpty(datasourceA.messageKeys)
        assertIsEmpty(datasourceB.messageKeys)
        // TODO assert conversation final state
    }

    /**
     * @see <a href="https://signal.org/docs/specifications/doubleratchet/#out-of-order-messages">
     *     Out-of-order messages</a>
     */
    @Test
    fun `Run advanced out-of-order case test`(): TestResult = runTest {
        val sharedInit = DRSharedSecret(random.nextBytes(keyLength))
        val datasourceA = PlainMapDoubleRatchetLocalDatasource("A")
        val engineAlice = DoubleRatchetEngine(
            doubleRatchetLocalDatasource = datasourceA,
            doubleRatchetKeyRepository = getRepository(),
        )
        val datasourceB = PlainMapDoubleRatchetLocalDatasource("B")
        val engineBob = DoubleRatchetEngine(
            doubleRatchetLocalDatasource = datasourceB,
            doubleRatchetKeyRepository = getRepository(),
        )

        // Bob invite Alice
        val bobToAliceInvitation: InvitationData = engineBob.createInvitation()

        // Alice Accept Bob invitation
        val aliceToBobConversationId: DoubleRatchetUUID =
            engineAlice.createNewConversationFromInvitation(bobToAliceInvitation.publicKey, sharedInit)

        // Alice send a message to Bob
        val aliceMessage1 = engineAlice.getSendData(conversationId = aliceToBobConversationId)

        // Bob receives the message
        val receivedBob1 = engineBob.getFirstReceiveKey(aliceMessage1.messageHeader, bobToAliceInvitation.conversationId, sharedInit)
        assertContentEquals(aliceMessage1.messageKey.value, receivedBob1.value)

        // Alice sends a message to bob again, but bob misses it
        val aliceMessage2 = engineAlice.getSendData(conversationId = aliceToBobConversationId) // Missed
        val bobMessage1 = engineBob.getSendData(conversationId = bobToAliceInvitation.conversationId)
        val receivedAlice1 = engineAlice.getReceiveKey(bobMessage1.messageHeader, aliceToBobConversationId)
        assertContentEquals(bobMessage1.messageKey.value, receivedAlice1.value)

        val bobMessage2 = engineBob.getSendData(conversationId = bobToAliceInvitation.conversationId)
        val receivedAlice2 = engineAlice.getReceiveKey(bobMessage2.messageHeader, aliceToBobConversationId)
        assertContentEquals(bobMessage2.messageKey.value, receivedAlice2.value)

        val aliceMessage3 = engineAlice.getSendData(conversationId = aliceToBobConversationId) // Missed
        val aliceMessage4 = engineAlice.getSendData(conversationId = aliceToBobConversationId) // RECEIVED
        val receivedBob4 = engineBob.getReceiveKey(aliceMessage4.messageHeader, bobToAliceInvitation.conversationId)
        assertContentEquals(aliceMessage4.messageKey.value, receivedBob4.value)
        val receivedBob3 = engineBob.getReceiveKey(aliceMessage3.messageHeader, bobToAliceInvitation.conversationId)
        val receivedBob2 = engineBob.getReceiveKey(
            aliceMessage2.messageHeader,
            bobToAliceInvitation.conversationId,
        ) // Finally Received
        assertContentEquals(aliceMessage2.messageKey.value, receivedBob2.value)
        assertContentEquals(aliceMessage3.messageKey.value, receivedBob3.value)

        assertIsEmpty(datasourceA.messageKeys)
        assertIsEmpty(datasourceB.messageKeys)
        // TODO assert conversation final state
    }

    private fun getRepository() = DoubleRatchetKeyRepositoryFactory.getRepository(random, keyLength)
}
