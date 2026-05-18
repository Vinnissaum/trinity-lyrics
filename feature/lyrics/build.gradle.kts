@file:OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:db"))
    implementation(project(":core:ui"))
    implementation(project(":feature:presentation"))
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material)
    implementation(compose.materialIconsExtended)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    implementation(libs.reorderable)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.sqldelight.driver.sqlite)
    testImplementation(compose.desktop.currentOs)
    testImplementation(compose.desktop.uiTestJUnit4)
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.11.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
