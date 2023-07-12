package studio.lunabee.doubleratchet.model

class DoubleRatchetError(val type: Type) : Error(type.message) {

    enum class Type(val message: String) {
        ConversationNotSetup("the conversation is not setup"),
        ConversationNotFound("the conversation doesn't exist"),
        MessageKeyNotFound("the message key is not found"),
        RequiredChainKeyMissing("can't setup conversation because the initial chainKey is missing from message header")
    }
}