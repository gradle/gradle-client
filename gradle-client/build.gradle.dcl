desktopComposeApp {
    group = "org.gradle.client"

    // Version must be strictly x.y.z and >= 1.0.0
    // for native packaging to work across platforms
    version = "1.1.3"

    kotlinApplication {
        dependencies {
            implementation(platform("org.jetbrains.kotlin:kotlin-bom:2.0.21"))
            implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.8.1"))
            implementation(platform("org.jetbrains.kotlinx:kotlinx-serialization-bom:1.6.3"))
        }

        targets {
            jvm {
                jdkVersion = 17

                dependencies {
                    implementation(project(":build-action"))
                    implementation(project(":mutations-demo"))

                    implementation("org.gradle:gradle-tooling-api:8.12-20241009055624+0000")

                    implementation("com.arkivanov.decompose:decompose:3.0.0")
                    implementation("com.arkivanov.decompose:extensions-compose:3.0.0")
                    implementation("com.arkivanov.essenty:lifecycle-coroutines:1.3.0")
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

                    implementation("io.ktor:ktor-client-okhttp:2.3.12")
                    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")

                    implementation("dev.chrisbanes.material3:material3-window-size-class-multiplatform:0.5.0")
                    implementation("com.materialkolor:material-kolor:1.7.0")
                    implementation("io.github.vinceglb:filekit-compose:0.8.2")

                    implementation("org.slf4j:slf4j-api:2.0.14")
                    implementation("ch.qos.logback:logback-classic:1.5.6")

                    implementation("org.gradle:gradle-declarative-dsl-core:8.14-20250222002553+0000")
                    implementation("org.gradle:gradle-declarative-dsl-evaluator:8.14-20250222002553+0000")
                    implementation("org.gradle:gradle-declarative-dsl-tooling-models:8.14-20250222002553+0000")

                    runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")

                    // TODO:DCL Compose doesn't play well with DCL SoftwareTypes
                    // But we can determine the value of these strings at runtime and just hardcode them
                    implementation("org.jetbrains.compose.runtime:runtime:1.6.11")
                    implementation("org.jetbrains.compose.foundation:foundation:1.6.11")
                    implementation("org.jetbrains.compose.material3:material3:1.6.11")
                    implementation("org.jetbrains.compose.material:material-icons-extended:1.6.11")
                    implementation("org.jetbrains.compose.ui:ui:1.6.11")
                    implementation("org.jetbrains.compose.components:components-resources:1.6.11")
                    implementation("org.jetbrains.compose.components:components-ui-tooling-preview:1.6.11")
                    implementation("org.jetbrains.compose.desktop:desktop-jvm-macos-arm64:1.6.11")
                }

                testing {
                    dependencies {
                        // TODO:DCL Compose doesn't play well with DCL SoftwareTypes
                        // But we can determine the value of this string at runtime and just hardcode it
                        implementation("org.jetbrains.compose.ui:ui-test-junit4:1.6.11")

                        runtimeOnly("org.junit.vintage:junit-vintage-engine:5.12.0")
                    }
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

        // TODO:DCL This could use a map model when one is available in DCL
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
                    optimize = false
                    obfuscate = false
                    configurationFiles = listOf(layout.projectDirectory.file("proguard-desktop.pro"))
                }
            }
        }

        nativeDistributions {
            // TODO:DCL Soon, we will be able to use unqualified enums in a list in DCL, but not yet
            targetFormats = listOf("Dmg", "Msi", "Deb")

            packageName = "GradleClient"
            description = "Gradle Client"
            vendor = "Gradle, Inc."
            // TODO:DCL We need to be able to add default imports in order to do: copyrightYear = Year.now()
            copyrightYear = "2025"
            appResourcesRootDir = layout.projectDirectory.dir("src/assets")

            modules = listOf(
                "java.instrument",
                "java.management",
                "java.naming",
                "java.scripting",
                "java.sql",
                "jdk.compiler",
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
