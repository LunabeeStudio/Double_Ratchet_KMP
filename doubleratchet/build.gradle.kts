plugins {
    kotlin("multiplatform")
    id("java-library")
    alias(libs.plugins.detekt)
    `lunabee-publish`
}

group = "studio.lunabee.doubleratchet"
description = "Kotlin multiplatform implementation of double ratchet algorithm"
version = "0.1.0"

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = ProjectConfig.JDK_VERSION.toString()
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
        withJava()
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "doubleratchet"
        }
    }

    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutine.test)
            }
        }
        val jvmMain by getting
        val jvmTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutine.test)
                implementation(libs.bouncycastle)
            }
        }
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
        val iosX64Test by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting
        val iosTest by creating {
            dependsOn(commonTest)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
        }
    }
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
