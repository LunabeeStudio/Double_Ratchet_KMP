package studio.lunabee.doubleratchet.utils

import platform.Foundation.NSUUID

actual fun provideRandomUUID(): String = NSUUID().UUIDString()