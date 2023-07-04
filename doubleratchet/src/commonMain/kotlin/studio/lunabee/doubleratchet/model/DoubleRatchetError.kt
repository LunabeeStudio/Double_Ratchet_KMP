package studio.lunabee.doubleratchet.model

class DoubleRatchetError(val type: Type) : Error(type.message) {

    enum class Type(val message: String) {
        ConversationNotSetup("The conversation setup is not done"),
        ConversationNotFound("the conversation doesn't exist"),
        MessageKeyNotFound("the message key is not found"),
        RequiredChainKeyMissing("can't setup conversation because initial chainKey is missing from message header")
    }
}