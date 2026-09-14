plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    // Must match app/build.gradle.kts's Java/Kotlin target (17) - a mismatch here means
    // scoring.jar is compiled to a newer bytecode version than javac accepts when
    // compiling the app module's Room-generated Java sources against it.
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
}
