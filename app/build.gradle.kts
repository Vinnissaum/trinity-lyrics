plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material)
    implementation(compose.materialIconsExtended)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    implementation(project(":core:domain"))
    implementation(project(":core:db"))
    implementation(project(":core:ui"))
    implementation(project(":feature:lyrics"))
    implementation(project(":feature:presentation"))
    implementation(project(":feature:import"))
    implementation(libs.sqldelight.driver.sqlite)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "dev.trinitychurch.lyrics.app.MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi)
            packageName = "Trinity Lyrics"
            packageVersion = "0.1.0"
            description = "Apresentação de letras para igrejas"
            copyright = "© 2026 Trinity Labs"
            vendor = "Trinity Labs"

            windows {
                menuGroup = "Trinity Lyrics"
                // upgradeUuid must never change after first release — identifies this product for MSI upgrades
                upgradeUuid = "F3A4B5C6-D7E8-4F9A-B0C1-D2E3F4A5B6C7"
                dirChooser = true
                perUserInstall = false
                shortcut = true
            }
        }
    }
}
