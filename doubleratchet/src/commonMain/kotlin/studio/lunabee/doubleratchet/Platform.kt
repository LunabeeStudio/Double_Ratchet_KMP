package studio.lunabee.doubleratchet

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform