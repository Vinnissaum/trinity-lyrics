plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.sqldelight)
}

sqldelight {
    databases {
        create("TrinityLyricsDatabase") {
            packageName.set("dev.trinitychurch.lyrics.db")
        }
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.sqldelight.driver.sqlite)
    implementation(libs.sqldelight.coroutines)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Phase 0: no migrations yet — disable all SQLDelight migration verification tasks
// to avoid native SQLite JNI incompatibility in this environment
afterEvaluate {
    tasks.matching { it.name.startsWith("verify") && it.name.contains("Migration") }.configureEach {
        enabled = false
    }
}
