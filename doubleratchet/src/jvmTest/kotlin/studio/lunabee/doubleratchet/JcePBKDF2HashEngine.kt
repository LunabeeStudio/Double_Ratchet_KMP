/*
 * Copyright (c) 2023-2023 Lunabee Studio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package studio.lunabee.doubleratchet

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.NoSuchAlgorithmException
import java.security.Security
import java.security.spec.KeySpec
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.security.auth.DestroyFailedException

class JcePBKDF2HashEngine {
    private val secretKeyFactory: SecretKeyFactory

    init {
        secretKeyFactory = try {
            getFactory()
        } catch (error: NoSuchAlgorithmException) {
            val bcProvider = BouncyCastleProvider()
            Security.removeProvider(bcProvider.name)
            val res = Security.addProvider(bcProvider)
            if (res == -1) {
                println("Failed to insert $bcProvider")
            }
            getFactory()
        }

        println("Initialize ${javaClass.simpleName} using ${secretKeyFactory.provider}")
    }

    fun deriveKey(key: ByteArray, salt: ByteArray): ByteArray {
        return doHash(key, salt)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun doHash(key: ByteArray, salt: ByteArray): ByteArray {
        return PBEKeySpec(key.joinToString().toCharArray(), salt, 1, DERIVED_KEY_LENGTH_BIT).use { pbeKeySpec ->
            secretKeyFactory.generateSecret(pbeKeySpec).use { secretKey ->
                secretKey.encoded
            }
        }
    }

    private fun getFactory() = SecretKeyFactory.getInstance(ALGORITHM)

    companion object {
        private const val ALGORITHM = "PBKDF2withHmacSHA512"
        private const val DERIVED_KEY_LENGTH_BIT = 256
    }
}

internal inline fun <T> PBEKeySpec.use(block: (KeySpec) -> T): T {
    return try {
        block(this)
    } finally {
        clearPassword()
    }
}

internal inline fun <T> SecretKey.use(block: (SecretKey) -> T): T {
    return try {
        block(this)
    } finally {
        this.safeDestroy()
    }
}

internal fun SecretKey.safeDestroy() {
    try {
        destroy()
    } catch (e: DestroyFailedException) {
        // Destroy not implemented
    } catch (e: NoSuchMethodError) {
        // Destroy not implemented
    }
}
