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

plugins {
    // trick: for the same plugin versions in all sub-modules
    kotlin("multiplatform").apply(false)
    alias(libs.plugins.detekt)
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

dependencies {
    detektPlugins(libs.detekt.formating)
}

detekt {
    parallel = true
    source.from(files(rootProject.rootDir))
    buildUponDefaultConfig = true
    config.from(files("${rootProject.rootDir}/lunabee-detekt-config.yml"))
    autoCorrect = true
    ignoreFailures = true
}

tasks.detekt.configure {
    outputs.upToDateWhen { false }
    exclude("**/build/**")
    reports {
        xml.required.set(true)
        xml.outputLocation.set(file("$buildDir/reports/detekt/detekt-report.xml"))

        html.required.set(true)
        html.outputLocation.set(file("$buildDir/reports/detekt/detekt-report.html"))
    }
}
