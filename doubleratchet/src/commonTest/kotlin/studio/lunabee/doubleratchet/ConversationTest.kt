package studio.lunabee.doubleratchet

import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import studio.lunabee.doubleratchet.model.DoubleRatchetError
import studio.lunabee.doubleratchet.model.DoubleRatchetUUID
import studio.lunabee.doubleratchet.model.InvitationData
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConversationTest {

    companion object {
        private val seed: Int = Random.nextInt().also {
            println("Random seed = $it")
        }
        private val random: Random = Random(seed)
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
        val sharedSecretAlice =
            cryptoRepository1.createDiffieHellmanSharedSecret(keyPairBob.publicKey, keyPairAlice.privateKey)
        val sharedSecretBob =
            cryptoRepository2.createDiffieHellmanSharedSecret(keyPairAlice.publicKey, keyPairBob.privateKey)

        assertContentEquals(sharedSecretAlice, sharedSecretBob)
    }

    /**
     * @see <a href="https://signal.org/docs/specifications/doubleratchet/#kdf-chains">
     *     KDF chains</a>
     */
    @Test
    fun `KDF algorithm test`(): TestResult = runTest {
        val chainKey = random.nextBytes(random.nextInt(200))
        val cryptoRepository1 = DoubleRatchetKeyRepositoryFactory.getRepository(random)
        val cryptoRepository2 = DoubleRatchetKeyRepositoryFactory.getRepository(random)
        val value1 = cryptoRepository1.deriveKey(chainKey)
        val value2 = cryptoRepository2.deriveKey(chainKey)

        assertContentEquals(value1.messageKey, value2.messageKey)
        assertContentEquals(value1.nextChainKey, value2.nextChainKey)
    }

    /**
     * @see <a href="https://signal.org/docs/specifications/doubleratchet/#double-ratchet">
     *     Double Ratchet</a>
     */
    @Test
    fun `Double Ratchet KDF+DH algorithm test`(): TestResult = runTest {
        val chainKey = random.nextBytes(random.nextInt(200))
        val chainKey2 = random.nextBytes(random.nextInt(200))
        val cryptoRepository1 = DoubleRatchetKeyRepositoryFactory.getRepository(random)
        val cryptoRepository2 = DoubleRatchetKeyRepositoryFactory.getRepository(random)
        val value1 = cryptoRepository1.deriveKeys(chainKey, chainKey2)
        val value2 = cryptoRepository2.deriveKeys(chainKey, chainKey2)

        assertContentEquals(value1.messageKey, value2.messageKey)
        assertContentEquals(value1.nextChainKey, value2.nextChainKey)
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
        assertContentEquals(aliceMessage1.messageKey, receivedBob1)
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
        assertContentEquals(aliceMessage1.messageKey, receivedBob1)
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
        assertContentEquals(aliceMessage1.messageKey, receivedBob1)
        val aliceMessage2 = engineAlice.getSendData(conversationId = aliceToBobConversationId)
        val receivedBob2 = engineBob.getReceiveKey(aliceMessage2.messageHeader, bobToAliceInvitation.conversationId)
        assertContentEquals(aliceMessage2.messageKey, receivedBob2)
        val aliceMessage3 = engineAlice.getSendData(conversationId = aliceToBobConversationId)
        val receivedBob3 = engineBob.getReceiveKey(aliceMessage3.messageHeader, bobToAliceInvitation.conversationId)
        assertContentEquals(aliceMessage3.messageKey, receivedBob3)
        val bobMessage1 = engineBob.getSendData(bobToAliceInvitation.conversationId)
        val receiveAlice1 = engineAlice.getReceiveKey(bobMessage1.messageHeader, aliceToBobConversationId)
        assertContentEquals(bobMessage1.messageKey, receiveAlice1)
        val bobMessage2 = engineBob.getSendData(bobToAliceInvitation.conversationId)
        val receiveAlice2 = engineAlice.getReceiveKey(bobMessage2.messageHeader, aliceToBobConversationId)
        assertContentEquals(bobMessage2.messageKey, receiveAlice2)
    }

    /**
     * @see <a href="https://signal.org/docs/specifications/doubleratchet/#out-of-order-messages">
     *     Out-of-order messages</a>
     */
    @Test
    fun `Run basic out-of-order case test`(): TestResult = runTest {
        val engineAlice = DoubleRatchetEngine(
            doubleRatchetLocalDatasource = PlainMapDoubleRatchetLocalDatasource(),
            doubleRatchetKeyRepository = DoubleRatchetKeyRepositoryFactory.getRepository(random),
        )
        val engineBob = DoubleRatchetEngine(
            doubleRatchetLocalDatasource = PlainMapDoubleRatchetLocalDatasource(),
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
        assertContentEquals(aliceMessage1.messageKey, receivedBob1)

        // Alice sends a message to bob again, but bob misses it
        val aliceMessage2 = engineAlice.getSendData(conversationId = aliceToBobConversationId) // Missed
        val aliceMessage3 = engineAlice.getSendData(conversationId = aliceToBobConversationId) // Missed
        val aliceMessage4 = engineAlice.getSendData(conversationId = aliceToBobConversationId) // RECEIVED
        val receivedBob4 = engineBob.getReceiveKey(aliceMessage4.messageHeader, bobToAliceInvitation.conversationId)
        assertContentEquals(aliceMessage4.messageKey, receivedBob4)
        val receivedBob3 = engineBob.getReceiveKey(aliceMessage3.messageHeader, bobToAliceInvitation.conversationId)
        val receivedBob2 = engineBob.getReceiveKey(
            aliceMessage2.messageHeader,
            bobToAliceInvitation.conversationId,
        ) // Finally Received
        assertContentEquals(aliceMessage2.messageKey, receivedBob2)
        assertContentEquals(aliceMessage3.messageKey, receivedBob3)
    }

    /**
     * @see <a href="https://signal.org/docs/specifications/doubleratchet/#out-of-order-messages">
     *     Out-of-order messages</a>
     */
    @Test
    fun `Run advanced out-of-order case test`(): TestResult = runTest {
        val engineAlice = DoubleRatchetEngine(
            doubleRatchetLocalDatasource = PlainMapDoubleRatchetLocalDatasource(),
            doubleRatchetKeyRepository = DoubleRatchetKeyRepositoryFactory.getRepository(random),
        )
        val engineBob = DoubleRatchetEngine(
            doubleRatchetLocalDatasource = PlainMapDoubleRatchetLocalDatasource(),
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
        assertContentEquals(aliceMessage1.messageKey, receivedBob1)

        // Alice sends a message to bob again, but bob misses it
        val aliceMessage2 = engineAlice.getSendData(conversationId = aliceToBobConversationId) // Missed
        val bobMessage1 = engineBob.getSendData(conversationId = bobToAliceInvitation.conversationId)
        val receivedAlice1 = engineAlice.getReceiveKey(bobMessage1.messageHeader, aliceToBobConversationId)
        assertContentEquals(bobMessage1.messageKey, receivedAlice1)

        val bobMessage2 = engineBob.getSendData(conversationId = bobToAliceInvitation.conversationId)
        val receivedAlice2 = engineAlice.getReceiveKey(bobMessage2.messageHeader, aliceToBobConversationId)
        assertContentEquals(bobMessage2.messageKey, receivedAlice2)

        val aliceMessage3 = engineAlice.getSendData(conversationId = aliceToBobConversationId) // Missed
        val aliceMessage4 = engineAlice.getSendData(conversationId = aliceToBobConversationId) // RECEIVED
        val receivedBob4 = engineBob.getReceiveKey(aliceMessage4.messageHeader, bobToAliceInvitation.conversationId)
        assertContentEquals(aliceMessage4.messageKey, receivedBob4)
        val receivedBob3 = engineBob.getReceiveKey(aliceMessage3.messageHeader, bobToAliceInvitation.conversationId)
        val receivedBob2 = engineBob.getReceiveKey(
            aliceMessage2.messageHeader,
            bobToAliceInvitation.conversationId,
        ) // Finally Received
        assertContentEquals(aliceMessage2.messageKey, receivedBob2)
        assertContentEquals(aliceMessage3.messageKey, receivedBob3)
    }
}
