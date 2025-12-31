kotlinApplication {
    group = "org.gradle.client"

    // Version must be strictly x.y.z and >= 1.0.0
    // for native packaging to work across platforms
    version = "1.1.3"

    dependencies {
        implementation(platform("org.jetbrains.kotlin:kotlin-bom:2.3.0"))
        implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.10.2"))
        implementation(platform("org.jetbrains.kotlinx:kotlinx-serialization-bom:1.9.0"))
    }

    targets {
        jvm {
            jdkVersion = 17

            dependencies {
                implementation(project(":build-action"))
                implementation(project(":mutations-demo"))

                implementation("org.gradle:gradle-tooling-api:9.4.0-milestone-4")

                implementation("com.arkivanov.decompose:decompose:3.3.0")
                implementation("com.arkivanov.decompose:extensions-compose:3.3.0")
                implementation("com.arkivanov.essenty:lifecycle-coroutines:2.5.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

                implementation("io.ktor:ktor-client-okhttp:3.3.0")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.0")

                implementation("dev.chrisbanes.material3:material3-window-size-class-multiplatform:0.5.0")
                implementation("com.materialkolor:material-kolor:3.0.1")
                implementation("io.github.vinceglb:filekit-dialogs-compose:0.11.0")

                implementation("org.slf4j:slf4j-api:2.0.17")
                implementation("ch.qos.logback:logback-classic:1.5.18")

                implementation("org.gradle:gradle-declarative-dsl-core:9.4.0-milestone-4")
                implementation("org.gradle:gradle-declarative-dsl-evaluator:9.4.0-milestone-4")
                implementation("org.gradle:gradle-declarative-dsl-tooling-models:9.4.0-milestone-4")
                implementation("org.jspecify:jspecify:1.0.0")

                runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")

                implementation("org.jetbrains.compose.runtime:runtime:1.9.0")
                implementation("org.jetbrains.compose.foundation:foundation:1.9.0")
                implementation("org.jetbrains.compose.material3:material3:1.10.0-alpha01")
                implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
                implementation("org.jetbrains.compose.ui:ui:1.9.0")
                implementation("org.jetbrains.compose.components:components-resources:1.9.0")
                implementation("org.jetbrains.compose.components:components-ui-tooling-preview:1.9.0")
                implementation("org.jetbrains.compose.desktop:desktop-jvm-macos-arm64:1.9.0")
                implementation("org.jetbrains.compose.desktop:desktop-jvm-linux-x64:1.9.0")
                implementation("org.jetbrains.compose.desktop:desktop-jvm-windows-x64:1.9.0")
            }

            testing {
                dependencies {
                    implementation("org.jetbrains.compose.ui:ui-test-junit4:1.9.0")

                    runtimeOnly("org.junit.vintage:junit-vintage-engine:5.13.4")
                    runtimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")
                }
            }
        }
    }

    sqlDelight {
        databases {
            database("ApplicationDatabase") {
                packageName = "org.gradle.client.core.database.sqldelight.generated"
                verifyDefinitions = true
                verifyMigrations = true
                deriveSchemaFromMigrations = true
                generateAsync = false
            }
        }
    }

    detekt {
        source = listOf(layout.projectDirectory.dir("src/jvmMain/kotlin"), layout.projectDirectory.dir("src/jvmTest/kotlin"))
        config = listOf(layout.settingsDirectory.file("gradle/detekt/detekt.conf"))
        parallel = true
    }

    compose {
        mainClass = "org.gradle.client.GradleClientMainKt"

        jvmArgs {
            jvmArg("-Xms") {
                value = "35m"
            }
            jvmArg("-Xmx") {
                value = "128m"
            }

            // This was originally added at an inner nesting level, but it's not clear why
            jvmArg("-splash") {
                value = ":\"\$APPDIR/resources/splash.png\""
            }
        }

        buildTypes {
            release {
                proguard {
                    version = "7.8.2"
                    optimize = false
                    obfuscate = false
                    configurationFiles = listOf(layout.projectDirectory.file("proguard-desktop.pro"))
                }
            }
        }

        nativeDistributions {
            targetFormats = listOf("Deb", "Dmg", "Msi", "Rpm")

            packageName = "GradleClient"
            description = "Gradle Client"
            vendor = "Gradle, Inc."
            copyrightYear = "2025"
            appResourcesRootDir = layout.projectDirectory.dir("src/assets")

            modules = listOf(
                "java.datatransfer",
                "java.desktop",
                "java.instrument",
                "java.management",
                "java.naming",
                "java.scripting",
                "java.sql",
                "jdk.compiler",
                "jdk.crypto.ec",
                "jdk.security.auth",
                "jdk.unsupported",
            )

            linux {
                iconFile = layout.projectDirectory.file("src/assets/desktop/icon.png")
            }
            macOS {
                appStore = false
                bundleID = "org.gradle.client"
                dockName = "Gradle Client"
                iconFile = layout.projectDirectory.file("src/assets/desktop/icon.icns")
            }
            windows {
                menu = true
                menuGroup = "" // root
                perUserInstall = true
                // https://wixtoolset.org/documentation/manual/v3/howtos/general/generate_guids.html
                upgradeUuid = "4E2A8498-9C66-4787-B063-AA7EA569FE99"
                iconFile = layout.projectDirectory.file("src/assets/desktop/icon.ico")
            }
        }
    }
}

