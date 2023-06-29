package studio.lunabee.doubleratchet

import org.junit.Test
import studio.lunabee.doubleratchet.crypto.CryptoRepositoryImpl
import studio.lunabee.doubleratchet.model.InvitationData

class AndroidGreetingTest {

    @Test
    fun testDiffieHellman() {
        val cryptoRepository1 = CryptoRepositoryImpl()
        val cryptoRepository2 = CryptoRepositoryImpl()
        val keyPairAlice = cryptoRepository1.generateKeyPair()
        val keyPairBob = cryptoRepository2.generateKeyPair()

        val sharedSecretAlice = cryptoRepository1.createDiffieHellmanSharedSecret(keyPairBob.publicKey, keyPairAlice.privateKey)
        val sharedSecretBob = cryptoRepository2.createDiffieHellmanSharedSecret(keyPairAlice.publicKey, keyPairBob.privateKey)
        assert(sharedSecretAlice.contentEquals(sharedSecretBob))
    }

    @Test
    fun testDerivationFunction() {
        val cryptoRepository1 = CryptoRepositoryImpl()
        val cryptoRepository2 = CryptoRepositoryImpl()
        val chainKey = cryptoRepository1.generateChainKey()
        val value1 = cryptoRepository1.deriveKey(chainKey)
        val value2 = cryptoRepository2.deriveKey(chainKey)
        assert(value1.messageKey.contentEquals(value2.messageKey))
        assert(value1.nextChainKey.contentEquals(value2.nextChainKey))
    }

    @Test
    fun testDerivationWithDHFunction() {
        val cryptoRepository1 = CryptoRepositoryImpl()
        val cryptoRepository2 = CryptoRepositoryImpl()
        val chainKey = cryptoRepository1.generateChainKey()
        val chainKey2 = cryptoRepository2.generateChainKey()

        val value1 = cryptoRepository1.deriveKeys(chainKey, chainKey2)
        val value2 = cryptoRepository2.deriveKeys(chainKey, chainKey2)
        assert(value1.messageKey.contentEquals(value2.messageKey))
        assert(value1.nextChainKey.contentEquals(value2.nextChainKey))
    }

    @Test
    fun handShakeTest() {
        val engineAlice = DoubleRatchetEngine(cryptoRepository = CryptoRepositoryImpl())
        val engineBob = DoubleRatchetEngine(cryptoRepository = CryptoRepositoryImpl())
        val bobToAliceInvitation: InvitationData = engineBob.createInvitation()
        val aliceToBobConversationId: String = engineAlice.createNewConversationFromInvitation(bobToAliceInvitation.publicKey)
        val aliceMessage1 = engineAlice.getSendData(conversationId = aliceToBobConversationId)
        val receivedBob1 = engineBob.getReceiveKey(aliceMessage1.messageHeader, bobToAliceInvitation.conversationId)
        assert(aliceMessage1.messageKey.contentEquals(receivedBob1))
        val aliceMessage2 = engineAlice.getSendData(conversationId = aliceToBobConversationId)
        val receivedBob2 = engineBob.getReceiveKey(aliceMessage2.messageHeader, bobToAliceInvitation.conversationId)
        assert(aliceMessage2.messageKey.contentEquals(receivedBob2))
        val aliceMessage3 = engineAlice.getSendData(conversationId = aliceToBobConversationId)
        val receivedBob3 = engineBob.getReceiveKey(aliceMessage3.messageHeader, bobToAliceInvitation.conversationId)
        assert(aliceMessage3.messageKey.contentEquals(receivedBob3))
        val bobMessage1 = engineBob.getSendData(bobToAliceInvitation.conversationId)
        val receiveAlice1 = engineAlice.getReceiveKey(bobMessage1.messageHeader, aliceToBobConversationId)
        assert(bobMessage1.messageKey.contentEquals(receiveAlice1))
        val bobMessage2 = engineBob.getSendData(bobToAliceInvitation.conversationId)
        val receiveAlice2 = engineAlice.getReceiveKey(bobMessage2.messageHeader, aliceToBobConversationId)
        assert(bobMessage2.messageKey.contentEquals(receiveAlice2))
    }

    @Test
    fun outOfOrderTest() {
        val engineAlice = DoubleRatchetEngine(cryptoRepository = CryptoRepositoryImpl())
        val engineBob = DoubleRatchetEngine(cryptoRepository = CryptoRepositoryImpl())

        // Bob invite Alice
        val bobToAliceInvitation: InvitationData = engineBob.createInvitation()

        // Alice Accept Bob invitation
        val aliceToBobConversationId: String = engineAlice.createNewConversationFromInvitation(bobToAliceInvitation.publicKey)

        // Alice send a message to Bob
        val aliceMessage1 = engineAlice.getSendData(conversationId = aliceToBobConversationId)

        // Bob receives the message
        val receivedBob1 = engineBob.getReceiveKey(aliceMessage1.messageHeader, bobToAliceInvitation.conversationId)
        assert(aliceMessage1.messageKey.contentEquals(receivedBob1))

        // Alice sends a message to bob again, but bob misses it
        val aliceMessage2 = engineAlice.getSendData(conversationId = aliceToBobConversationId) // Missed
        val aliceMessage3 = engineAlice.getSendData(conversationId = aliceToBobConversationId) // Missed
        val aliceMessage4 = engineAlice.getSendData(conversationId = aliceToBobConversationId) // RECEIVED
        val receivedBob4 = engineBob.getReceiveKey(aliceMessage4.messageHeader, bobToAliceInvitation.conversationId)
        assert(aliceMessage4.messageKey.contentEquals(receivedBob4))
        val receivedBob3 = engineBob.getReceiveKey(aliceMessage3.messageHeader, bobToAliceInvitation.conversationId)
        val receivedBob2 = engineBob.getReceiveKey(aliceMessage2.messageHeader, bobToAliceInvitation.conversationId) // Finally Received
        assert(aliceMessage2.messageKey.contentEquals(receivedBob2))
        assert(aliceMessage3.messageKey.contentEquals(receivedBob3))
    }

    @Test
    fun outOfOrderComplicatedTest() {
        val engineAlice = DoubleRatchetEngine(cryptoRepository = CryptoRepositoryImpl())
        val engineBob = DoubleRatchetEngine(cryptoRepository = CryptoRepositoryImpl())

        // Bob invite Alice
        val bobToAliceInvitation: InvitationData = engineBob.createInvitation()

        // Alice Accept Bob invitation
        val aliceToBobConversationId: String = engineAlice.createNewConversationFromInvitation(bobToAliceInvitation.publicKey)

        // Alice send a message to Bob
        val aliceMessage1 = engineAlice.getSendData(conversationId = aliceToBobConversationId)

        // Bob receives the message
        val receivedBob1 = engineBob.getReceiveKey(aliceMessage1.messageHeader, bobToAliceInvitation.conversationId)
        assert(aliceMessage1.messageKey.contentEquals(receivedBob1))

        // Alice sends a message to bob again, but bob misses it
        val aliceMessage2 = engineAlice.getSendData(conversationId = aliceToBobConversationId) // Missed
        val bobMessage1 = engineBob.getSendData(conversationId = bobToAliceInvitation.conversationId)
        val receivedAlice1 = engineAlice.getReceiveKey(bobMessage1.messageHeader, aliceToBobConversationId)
        assert(bobMessage1.messageKey.contentEquals(receivedAlice1))

        val bobMessage2 = engineBob.getSendData(conversationId = bobToAliceInvitation.conversationId)
        val receivedAlice2 = engineAlice.getReceiveKey(bobMessage2.messageHeader, aliceToBobConversationId)
        assert(bobMessage2.messageKey.contentEquals(receivedAlice2))

        val aliceMessage3 = engineAlice.getSendData(conversationId = aliceToBobConversationId) // Missed
        val aliceMessage4 = engineAlice.getSendData(conversationId = aliceToBobConversationId) // RECEIVED
        val receivedBob4 = engineBob.getReceiveKey(aliceMessage4.messageHeader, bobToAliceInvitation.conversationId)
        assert(aliceMessage4.messageKey.contentEquals(receivedBob4))
        val receivedBob3 = engineBob.getReceiveKey(aliceMessage3.messageHeader, bobToAliceInvitation.conversationId)
        val receivedBob2 = engineBob.getReceiveKey(aliceMessage2.messageHeader, bobToAliceInvitation.conversationId) // Finally Received
        assert(aliceMessage2.messageKey.contentEquals(receivedBob2))
        assert(aliceMessage3.messageKey.contentEquals(receivedBob3))
    }
}