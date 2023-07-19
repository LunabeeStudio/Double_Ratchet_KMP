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

package studio.lunabee.doubleratchet.model

import platform.Foundation.NSUUID

actual class DoubleRatchetUUID(val uuid: NSUUID) {
    actual fun uuidString(): String = uuid.UUIDString()

    override fun equals(other: Any?): Boolean {
        return other is DoubleRatchetUUID && other.uuid.isEqual(uuid)
    }

    override fun hashCode(): Int = uuid.UUIDString().hashCode()
}

actual fun createRandomUUID(): DoubleRatchetUUID = DoubleRatchetUUID(NSUUID())
