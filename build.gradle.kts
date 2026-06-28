@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    id("org.jetbrains.kotlin.multiplatform") version "2.3.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20"
    id("com.google.devtools.ksp") version "2.3.9"
    id("org.jetbrains.compose") version "1.11.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20"
}

group = "com.github.adamyork"
version = "0.0.1"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    wasmJs {
        binaries.executable()
        browser {
            commonWebpackConfig {
                devServer = (devServer
                    ?: org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.DevServer()).apply {
                    static(project.file("build/dist/wasmJs/productionExecutable").absolutePath)
                }
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("me.tatarka.inject:kotlin-inject-runtime:0.7.2")
            implementation("io.github.reactivecircus.cache4k:cache4k:0.14.0")
            implementation("io.github.oshai:kotlin-logging:8.0.4")
            implementation("org.jetbrains.compose.runtime:runtime:1.11.1")
            implementation("org.jetbrains.compose.foundation:foundation:1.11.1")
            implementation("org.jetbrains.compose.ui:ui:1.11.1")
            implementation("org.jetbrains.compose.material3:material3:1.9.0")
            implementation("org.jetbrains.compose.components:components-resources:1.11.1")
            implementation("io.coil-kt.coil3:coil-compose:3.0.4")
            implementation("io.coil-kt.coil3:coil-network-ktor3:3.0.4")
            implementation("io.ktor:ktor-client-js:3.0.1")
            implementation("com.charleskorn.kaml:kaml:0.104.0")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
        }

        wasmJsMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-browser:0.3")
        }
    }
}

val prepareDevServer = tasks.register<Copy>("prepareDevServer") {
    description = ""
    from("src/wasmJsMain/web")
    into(layout.buildDirectory.dir("dist/wasmJs/productionExecutable"))
}

tasks.matching { it.name.contains("wasmJsBrowserDevelopmentRun") }.configureEach {
    dependsOn(prepareDevServer)
}

dependencies {
    add("kspWasmJs", "me.tatarka.inject:kotlin-inject-compiler-ksp:0.7.2")
}