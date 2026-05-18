@file:OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:db"))
    implementation(project(":core:ui"))
    implementation(libs.kotlinx.serialization.json)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material)
    implementation(compose.materialIconsExtended)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.sqldelight.driver.sqlite)
    testImplementation(compose.desktop.currentOs)
    testImplementation(compose.desktop.uiTestJUnit4)
    testImplementation(libs.junit4)
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.11.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
