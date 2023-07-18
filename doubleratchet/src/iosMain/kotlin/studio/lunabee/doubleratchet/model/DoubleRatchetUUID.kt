package studio.lunabee.doubleratchet.model

import platform.Foundation.NSUUID

actual class DoubleRatchetUUID(val uuid: NSUUID) {
    actual fun uuidString(): String = uuid.UUIDString()

    override fun equals(other: Any?): Boolean {
        return other is DoubleRatchetUUID && other.uuid.isEqual(uuid)
    }

    override fun hashCode(): Int = uuid.UUIDString().hashCode()
}

actual fun createRandomUUID(): DoubleRatchetUUID = DoubleRatchetUUID(NSUUID())
