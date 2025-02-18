@file:Suppress("UnstableApiUsage")
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.time.Year

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

desktopComposeApp {
    group = "org.gradle.client"

    // Version must be strictly x.y.z and >= 1.0.0
    // for native packaging to work across platforms
    version = "1.1.3"

    kotlinApplication {
        dependencies {
            implementation(project.dependencies.platform(libs.kotlin.bom))
            implementation(project.dependencies.platform(libs.kotlinx.coroutines.bom))
            implementation(project.dependencies.platform(libs.kotlinx.serialization.bom))
            implementation(project.dependencies.platform(libs.ktor.bom))
        }

        targets {
            jvm {
                jdkVersion = 17

                dependencies {
                    implementation(project(":build-action"))
                    implementation(project(":mutations-demo"))

                    implementation(libs.gradle.tooling.api)

                    implementation(libs.decompose.decompose)
                    implementation(libs.decompose.compose)
                    implementation(libs.essenty.lifecycle.coroutines)
                    implementation(libs.kotlinx.serialization.json)

                    implementation(libs.ktor.client.okhttp)
                    implementation(libs.ktor.serialization.kotlinx.json)

                    implementation(libs.material3WindowSizeClassMultiplatform)
                    implementation(libs.materialKolor)
                    implementation(libs.filekit.compose)

                    implementation(libs.slf4j.api)
                    implementation(libs.logback.classic)

                    implementation(libs.gradle.declarative.dsl.core)
                    implementation(libs.gradle.declarative.dsl.evaluator)
                    implementation(libs.gradle.declarative.dsl.tooling.models)

                    runtimeOnly(libs.kotlinx.coroutines.swing)

                    // TODO: Load these all into the VC
                    // Compose doesn't play well with DCL SoftwareTypes
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
                        implementation(libs.junit.junit)

                        // Compose doesn't play well with DCL SoftwareTypes
                        // But we can determine the value of this string at runtime and just hardcode it
                        implementation("org.jetbrains.compose.ui:ui-test-junit4:1.6.11")

                        runtimeOnly(libs.junit.jupiter.engine)
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
        source.setFrom("src/jvmMain/kotlin", "src/jvmTest/kotlin")
        config.setFrom(rootDir.resolve("gradle/detekt/detekt.conf"))
        parallel = true
    }

    compose {
        mainClass = "org.gradle.client.GradleClientMainKt"

        // TODO: This should use a simpler collection model when one is available in DCL
        jvmArgs {
            jvmArg("-Xms") {
                value = "35m"
            }
            jvmArg("-Xmx") {
                value = "128m"
            }

            // This was originally added at an inner nesting level, but it's not clear why
            jvmArg("-splash") {
                value = "\${'$'}APPDIR/resources/splash.png"
            }
        }

        buildTypes {
            release {
                proguard {
                    optimize = false
                    obfuscate = false
                    configurationFiles.setFrom(layout.projectDirectory.file("proguard-desktop.pro"))
                }
            }
        }

        nativeDistributions {
            // TODO: This should use a simpler collection model when one is available in DCL
            targetFormats {
                targetFormat("dmg") {
                    value = TargetFormat.Dmg
                }
                targetFormat("mis") {
                    value = TargetFormat.Msi
                }
                targetFormat("deb") {
                    value = TargetFormat.Deb
                }
            }

            packageName = "GradleClient"
            packageVersion = "1.1.3"
            description = "Gradle Client"
            vendor = "Gradle"
            copyrightYear = Year.now()
            appResourcesRootDir = layout.projectDirectory.dir("src/assets")

            // TODO: This should use a simpler collection model when one is available in DCL
            modules {
                module("java.instrument") {}
                module("java.management") {}
                module("java.naming") {}
                module("java.scripting") {}
                module("java.sql") {}
                module("jdk.compiler") {}
                module("jdk.security.auth") {}
                module("jdk.unsupported") {}
            }

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
                upgradeUuidFile = layout.projectDirectory.file("app-uuid.txt")
                iconFile = layout.projectDirectory.file("src/assets/desktop/icon.ico")
            }
        }
    }
}
