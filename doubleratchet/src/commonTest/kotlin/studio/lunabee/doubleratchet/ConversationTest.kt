package studio.lunabee.doubleratchet

import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import studio.lunabee.doubleratchet.model.ChainKey
import studio.lunabee.doubleratchet.model.DoubleRatchetError
import studio.lunabee.doubleratchet.model.DoubleRatchetUUID
import studio.lunabee.doubleratchet.model.InvitationData
import studio.lunabee.doubleratchet.model.SharedSecret
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConversationTest {

    companion object {
        private val random: Random = RandomProviderTest.random
    }

    /**
     * @see <a href="https://signal.org/docs/specifications/doubleratchet/#diffie-hellman-ratchet">
     *     Diffie-Hellman ratchet</a>
     */
    @Test
    fun `DH shared secret algorithm test`(): TestResult = runTest {
        val cryptoRepository1 = DoubleRatchetKeyRepositoryFactory.getRepository(random)
        val cryptoRepository2 = DoubleRatchetKeyRepositoryFactory.getRepository(random)
        val keyPairAlice = cryptoRepository1.generateKeyPair()
        val keyPairBob = cryptoRepository2.generateKeyPair()
        val sharedSecretAlice = SharedSecret(random.nextBytes(SharedSecret.SECRET_LENGTH_BYTE))
        val sharedSecretBob = SharedSecret(random.nextBytes(SharedSecret.SECRET_LENGTH_BYTE))

        cryptoRepository1.createDiffieHellmanSharedSecret(keyPairBob.publicKey, keyPairAlice.privateKey, sharedSecretAlice)
        cryptoRepository2.createDiffieHellmanSharedSecret(keyPairAlice.publicKey, keyPairBob.privateKey, sharedSecretBob)

        assertContentEquals(sharedSecretAlice.value, sharedSecretBob.value)
    }

    /**
     * @see <a href="https://signal.org/docs/specifications/doubleratchet/#kdf-chains">
     *     KDF chains</a>
     */
    @Test
    fun `KDF algorithm test`(): TestResult = runTest {
        val chainKey = ChainKey.random(random)
        val cryptoRepository1 = DoubleRatchetKeyRepositoryFactory.getRepository(random)
        val cryptoRepository2 = DoubleRatchetKeyRepositoryFactory.getRepository(random)
        val value1 = cryptoRepository1.deriveKey(chainKey)
        val value2 = cryptoRepository2.deriveKey(chainKey)

        assertContentEquals(value1.messageKey.value, value2.messageKey.value)
        assertContentEquals(value1.chainKey.value, value2.chainKey.value)
    }

    /**
     * @see <a href="https://signal.org/docs/specifications/doubleratchet/#double-ratchet">
     *     Double Ratchet</a>
     */
    @Test
    fun `Double Ratchet KDF+DH algorithm test`(): TestResult = runTest {
        val chainKey = ChainKey.random(random)
        val sharedSecret = SharedSecret(random.nextBytes(SharedSecret.SECRET_LENGTH_BYTE))
        val cryptoRepository1 = DoubleRatchetKeyRepositoryFactory.getRepository(random)
        val cryptoRepository2 = DoubleRatchetKeyRepositoryFactory.getRepository(random)
        val value1 = cryptoRepository1.deriveKeys(chainKey, sharedSecret)
        val value2 = cryptoRepository2.deriveKeys(chainKey, sharedSecret)

        assertContentEquals(value1.messageKey.value, value2.messageKey.value)
        assertContentEquals(value1.chainKey.value, value2.chainKey.value)
    }

    @Test
    fun `Run conversation flow test`(): TestResult = runTest {
        val engineAlice = DoubleRatchetEngine(
            doubleRatchetLocalDatasource = PlainMapDoubleRatchetLocalDatasource(),
            doubleRatchetKeyRepository = DoubleRatchetKeyRepositoryFactory.getRepository(random),
        )
        val engineBob = DoubleRatchetEngine(
            doubleRatchetLocalDatasource = PlainMapDoubleRatchetLocalDatasource(),
            doubleRatchetKeyRepository = DoubleRatchetKeyRepositoryFactory.getRepository(random),
        )
        val bobToAliceInvitation: InvitationData = engineBob.createInvitation()
        val aliceToBobConversationId: DoubleRatchetUUID =
            engineAlice.createNewConversationFromInvitation(bobToAliceInvitation.publicKey)
        val error = assertFailsWith(DoubleRatchetError::class) {
            engineBob.getSendData(conversationId = bobToAliceInvitation.conversationId)
        }
        assertEquals(DoubleRatchetError.Type.ConversationNotSetup, error.type)
        val aliceMessage1 = engineAlice.getSendData(conversationId = aliceToBobConversationId)
        val receivedBob1 = engineBob.getReceiveKey(aliceMessage1.messageHeader, bobToAliceInvitation.conversationId)
        assertContentEquals(aliceMessage1.messageKey.value, receivedBob1.value)
    }

    @Test
    fun `Decrypt already decrypted message failure test`(): TestResult = runTest {
        val engineAlice = DoubleRatchetEngine(
            doubleRatchetLocalDatasource = PlainMapDoubleRatchetLocalDatasource(),
            doubleRatchetKeyRepository = DoubleRatchetKeyRepositoryFactory.getRepository(random),
        )
        val engineBob = DoubleRatchetEngine(
            doubleRatchetLocalDatasource = PlainMapDoubleRatchetLocalDatasource(),
            doubleRatchetKeyRepository = DoubleRatchetKeyRepositoryFactory.getRepository(random),
        )
        val bobToAliceInvitation: InvitationData = engineBob.createInvitation()
        val aliceToBobConversationId: DoubleRatchetUUID =
            engineAlice.createNewConversationFromInvitation(bobToAliceInvitation.publicKey)
        val aliceMessage1 = engineAlice.getSendData(conversationId = aliceToBobConversationId)
        val receivedBob1 = engineBob.getReceiveKey(aliceMessage1.messageHeader, bobToAliceInvitation.conversationId)
        assertContentEquals(aliceMessage1.messageKey.value, receivedBob1.value)
        val receivedBis = assertFailsWith(DoubleRatchetError::class) {
            engineBob.getReceiveKey(aliceMessage1.messageHeader, bobToAliceInvitation.conversationId)
        }
        assertEquals(DoubleRatchetError.Type.MessageKeyNotFound, receivedBis.type)
    }

    @Test
    fun `Run handshake flow test`(): TestResult = runTest {
        val engineAlice = DoubleRatchetEngine(
            doubleRatchetLocalDatasource = PlainMapDoubleRatchetLocalDatasource(),
            doubleRatchetKeyRepository = DoubleRatchetKeyRepositoryFactory.getRepository(random),
        )
        val engineBob = DoubleRatchetEngine(
            doubleRatchetLocalDatasource = PlainMapDoubleRatchetLocalDatasource(),
            doubleRatchetKeyRepository = DoubleRatchetKeyRepositoryFactory.getRepository(random),
        )
        val bobToAliceInvitation: InvitationData = engineBob.createInvitation()
        val aliceToBobConversationId: DoubleRatchetUUID =
            engineAlice.createNewConversationFromInvitation(bobToAliceInvitation.publicKey)
        val aliceMessage1 = engineAlice.getSendData(conversationId = aliceToBobConversationId)
        val receivedBob1 = engineBob.getReceiveKey(aliceMessage1.messageHeader, bobToAliceInvitation.conversationId)
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
        val datasourceA = PlainMapDoubleRatchetLocalDatasource()
        val engineAlice = DoubleRatchetEngine(
            doubleRatchetLocalDatasource = datasourceA,
            doubleRatchetKeyRepository = DoubleRatchetKeyRepositoryFactory.getRepository(random),
        )
        val datasourceB = PlainMapDoubleRatchetLocalDatasource()
        val engineBob = DoubleRatchetEngine(
            doubleRatchetLocalDatasource = datasourceB,
            doubleRatchetKeyRepository = DoubleRatchetKeyRepositoryFactory.getRepository(random),
        )

        // Bob invite Alice
        val bobToAliceInvitation: InvitationData = engineBob.createInvitation()

        // Alice Accept Bob invitation
        val aliceToBobConversationId: DoubleRatchetUUID =
            engineAlice.createNewConversationFromInvitation(bobToAliceInvitation.publicKey)

        // Alice send a message to Bob
        val aliceMessage1 = engineAlice.getSendData(conversationId = aliceToBobConversationId)

        // Bob receives the message
        val receivedBob1 = engineBob.getReceiveKey(aliceMessage1.messageHeader, bobToAliceInvitation.conversationId)
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

        assertEquals(1, datasourceA.chainKeys.size) // Alice did not receive message
        assertIsEmpty(datasourceA.messageKeys)
        assertIsEmpty(datasourceB.chainKeys)
        assertIsEmpty(datasourceB.messageKeys)
    }

    /**
     * @see <a href="https://signal.org/docs/specifications/doubleratchet/#out-of-order-messages">
     *     Out-of-order messages</a>
     */
    @Test
    fun `Run advanced out-of-order case test`(): TestResult = runTest {
        val datasourceA = PlainMapDoubleRatchetLocalDatasource()
        val engineAlice = DoubleRatchetEngine(
            doubleRatchetLocalDatasource = datasourceA,
            doubleRatchetKeyRepository = DoubleRatchetKeyRepositoryFactory.getRepository(random),
        )
        val datasourceB = PlainMapDoubleRatchetLocalDatasource()
        val engineBob = DoubleRatchetEngine(
            doubleRatchetLocalDatasource = datasourceB,
            doubleRatchetKeyRepository = DoubleRatchetKeyRepositoryFactory.getRepository(random),
        )

        // Bob invite Alice
        val bobToAliceInvitation: InvitationData = engineBob.createInvitation()

        // Alice Accept Bob invitation
        val aliceToBobConversationId: DoubleRatchetUUID =
            engineAlice.createNewConversationFromInvitation(bobToAliceInvitation.publicKey)

        // Alice send a message to Bob
        val aliceMessage1 = engineAlice.getSendData(conversationId = aliceToBobConversationId)

        // Bob receives the message
        val receivedBob1 = engineBob.getReceiveKey(aliceMessage1.messageHeader, bobToAliceInvitation.conversationId)
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

        assertIsEmpty(datasourceA.chainKeys)
        assertIsEmpty(datasourceA.messageKeys)
        assertIsEmpty(datasourceB.chainKeys)
        assertIsEmpty(datasourceB.messageKeys)
    }
}
