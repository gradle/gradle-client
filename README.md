# Gradle Client

![GitHub License](https://img.shields.io/github/license/eskatos/gradle-client)
![GitHub Top Language](https://img.shields.io/github/languages/top/eskatos/gradle-client)
![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/eskatos/gradle-client/ci.yml)
![GitHub Issues](https://img.shields.io/github/issues/eskatos/gradle-client)

This is a desktop application acting as a Gradle Tooling API client.

The build requires Java 17 with `jlink` and `jpackage` JDK tools.
The build will fail to configure with the wrong Java version.
Building release distributables will fail if the required JDK tools are not available.

```shell
# Run from sources
./gradlew :gradle-client:run

# Run from sources in continuous mode
./gradlew -t :gradle-client:run

# Run debug build type from build installation
./gradlew :gradle-client:runDistributable

# Run release build type from build installation
./gradlew :gradle-client:runReleaseDistributable
```

To add more actions start from [GetModelAction.kt](./gradle-client/src/jvmMain/kotlin/org/gradle/client/ui/connected/actions/GetModelAction.kt).

Packaging native distributions is platform dependent:

```shell
# Package DMG on MacOS
./gradlew :gradle-client:packageReleaseDmg

# Package DEB on Linux
./gradlew :gradle-client:packageReleaseDeb

# Package MSI on Windows
./gradlew :gradle-client:packageReleaseMsi
```

The GitHub Actions based CI builds the native distributions.
They are attached as artifacts to each workflow run.
