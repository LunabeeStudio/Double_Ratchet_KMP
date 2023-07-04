package studio.lunabee.doubleratchet.utils

import java.util.UUID

actual fun provideRandomUUID(): String {
    return UUID.randomUUID().toString()
}