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

import studio.lunabee.doubleratchet.model.Conversation

fun Conversation.print(): String {
    return "Conversation(\n" +
        "\tid=${id.uuidString()}\n" +
        "\tpersonalKeyPair=(\n" +
        "\t\tprivate=${personalKeyPair.privateKey.value.contentHashCode()}\n" +
        "\t\tpublic=${personalKeyPair.publicKey.value.contentHashCode()}\n" +
        "\t)\n" +
        "\trootKey=${rootKey?.value.contentHashCode()}\n" +
        "\tsendingChainKey=${sendingChainKey?.value.contentHashCode()}\n" +
        "\treceiveChainKey=${receiveChainKey?.value.contentHashCode()}\n" +
        "\tlastContactPublicKey=${lastContactPublicKey?.value.contentHashCode()}\n" +
        "\tnextSendMessageData=(\n" +
        "\t\tmessage=${nextSendMessageData.message}\n" +
        "\t\tsequence=${nextSendMessageData.sequence}\n" +
        "\t)\n" +
        "\treceivedLastMessageNumber=$receivedLastMessageNumber\n" +
        ")"
}
