@file:OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material)

    testImplementation(compose.desktop.uiTestJUnit4)
    testRuntimeOnly(compose.desktop.currentOs)
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
