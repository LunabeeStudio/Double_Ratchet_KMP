package studio.lunabee.doubleratchet

import studio.lunabee.doubleratchet.crypto.DoubleRatchetKeyRepository
import kotlin.random.Random

expect object DoubleRatchetKeyRepositoryFactory {
    fun getRepository(random: Random): DoubleRatchetKeyRepository
}
