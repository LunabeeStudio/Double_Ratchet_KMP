/*
 * Copyright (c) 2023 Lunabee Studio
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

import org.bouncycastle.crypto.digests.SHA512Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters

class BouncyCastleHKDFHashEngine {
    private val hkdf = HKDFBytesGenerator(SHA512Digest())

    // TODO add info like Rust impl
    //  https://github.com/signalapp/libsignal/blob/3b7f3173cc4431bc4c6e55f6182a37229b2db6fd/rust/protocol/src/ratchet/keys.rs#L22
    //  https://github.com/signalapp/libsignal/blob/3b7f3173cc4431bc4c6e55f6182a37229b2db6fd/rust/protocol/src/ratchet/keys.rs#L128
    fun deriveKey(key: ByteArray, salt: ByteArray, out: ByteArray = ByteArray(DERIVED_KEY_LENGTH_BYTE)): ByteArray {
        val hkdfParams = HKDFParameters(key, salt, null)
        hkdf.init(hkdfParams)
        hkdf.generateBytes(out, 0, out.size)
        return out
    }

    companion object {
        const val DERIVED_KEY_LENGTH_BYTE = 32
    }
}
