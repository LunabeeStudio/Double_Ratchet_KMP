package studio.lunabee.doubleratchet

import kotlin.random.Random

object RandomProviderTest {
    private val seed: Int = Random.nextInt().also {
        println("Random seed = $it")
    }
    val random: Random = Random(seed)
}
