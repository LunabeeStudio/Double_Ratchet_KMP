package studio.lunabee.doubleratchet.model

expect class DoubleRatchetUUID {
    fun uuidString(): String
}

expect fun createRandomUUID(): DoubleRatchetUUID