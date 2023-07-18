plugins {
    // trick: for the same plugin versions in all sub-modules
    kotlin("multiplatform").apply(false)
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
