package studio.lunabee.doubleratchet

import studio.lunabee.doubleratchet.crypto.DoubleRatchetKeyRepository
import kotlin.random.Random

expect class TestDoubleRatchetKeyRepository(
    random: Random,
) : DoubleRatchetKeyRepository
