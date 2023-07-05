package studio.lunabee.doubleratchet.model

import java.util.UUID

actual class DoubleRatchetUUID(val uuid: UUID) {
    actual fun uuidString(): String = uuid.toString()

    override fun equals(other: Any?): Boolean {
        return other is DoubleRatchetUUID && uuid == other.uuid
    }

    override fun hashCode(): Int = uuid.toString().hashCode()
}

actual fun createRandomUUID(): DoubleRatchetUUID {
    return DoubleRatchetUUID(uuid = UUID.randomUUID())
}