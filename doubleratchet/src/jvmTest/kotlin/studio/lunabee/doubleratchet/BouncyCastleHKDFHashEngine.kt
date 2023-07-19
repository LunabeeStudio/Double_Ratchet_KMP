package studio.lunabee.doubleratchet

import org.bouncycastle.crypto.digests.SHA512Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters

class BouncyCastleHKDFHashEngine {
    private val hkdf = HKDFBytesGenerator(SHA512Digest())

    fun deriveKey(key: ByteArray, salt: ByteArray, out: ByteArray = ByteArray(DERIVED_KEY_LENGTH_BYTE)): ByteArray {
        val hkdfParams = HKDFParameters(key, salt, null)
        hkdf.init(hkdfParams)
        hkdf.generateBytes(out, 0, DERIVED_KEY_LENGTH_BYTE)
        return out
    }

    companion object {
        const val DERIVED_KEY_LENGTH_BYTE = 32
    }
}
