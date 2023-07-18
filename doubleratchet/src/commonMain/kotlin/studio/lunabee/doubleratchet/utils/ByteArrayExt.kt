package studio.lunabee.doubleratchet.utils

import kotlin.random.Random

fun ByteArray.randomize(): ByteArray {
    Random.nextBytes(this)
    return this
}
