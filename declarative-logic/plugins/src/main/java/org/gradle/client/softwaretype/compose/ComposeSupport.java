package org.gradle.client.softwaretype.compose;

import kotlin.Unit;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;
import org.gradle.client.softwaretype.CustomDesktopComposeApplication;
import org.jetbrains.compose.ComposeExtension;
import org.jetbrains.compose.desktop.DesktopExtension;
import org.jetbrains.compose.desktop.application.dsl.*;
import org.jetbrains.compose.desktop.application.dsl.TargetFormat;
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.stream.Collectors;

public final class ComposeSupport {
    private ComposeSupport() { /* not instantiable */ }

    public static void wireCompose(Project project, CustomDesktopComposeApplication projectDefinition) {
        project.getPluginManager().apply("org.jetbrains.kotlin.plugin.compose");

        project.afterEvaluate(p -> {
            project.getPluginManager().apply("org.jetbrains.compose");
            ComposeExtension compose = project.getExtensions().getByType(ComposeExtension.class);
            DesktopExtension desktop = compose.getExtensions().getByType(DesktopExtension.class);
            JvmApplication application = desktop.getApplication();
            JvmApplicationDistributions nativeDistributions = application.getNativeDistributions();

            Compose composeDefinition = projectDefinition.getCompose();
            application.setMainClass(composeDefinition.getMainClass().get());
            composeDefinition.getJvmArgs().forEach(jvmArg -> application.getJvmArgs().add(jvmArg.getName() + jvmArg.getValue().get()));

            wireBuildTypes(composeDefinition, application.getBuildTypes());

            wireNativeDistribution(composeDefinition.getNativeDistributions(), nativeDistributions);

            // Need to set the main class for the JVM run task in the KMP model from the one nested in the Compose model
            KotlinMultiplatformExtension kotlin = project.getExtensions().getByType(KotlinMultiplatformExtension.class);
            kotlin.jvm("jvm" , jvm -> {
                jvm.mainRun(kotlinJvmRunDsl -> {
                    kotlinJvmRunDsl.getMainClass().set(composeDefinition.getMainClass());
                    return Unit.INSTANCE;
                });
            });
        });
    }

    private static void wireBuildTypes(Compose composeDefinition, JvmApplicationBuildTypes buildTypes) {
        wireBuildType(composeDefinition.getBuildTypes().getRelease(), buildTypes.getRelease());
    }

    private static void wireBuildType(BuildType buildTypeDefinition, JvmApplicationBuildType buildType) {
        ProguardSettings proguard = buildType.getProguard();
        Proguard proguardDefinition = buildTypeDefinition.getProguard();
        proguard.getOptimize().set(proguardDefinition.getOptimize());
        proguard.getObfuscate().set(proguardDefinition.getObfuscate());

        proguardDefinition.getConfigurationFiles().get().forEach(f -> proguard.getConfigurationFiles().from(f));
    }

    private static void wireNativeDistribution(NativeDistributions nativeDistributionsDefinition, JvmApplicationDistributions nativeDistributions) {
        nativeDistributions.setTargetFormats(EnumSet.copyOf(nativeDistributionsDefinition.getTargetFormats().get().stream().map(TargetFormat::valueOf).collect(Collectors.toList())));

        nativeDistributions.setPackageName(nativeDistributionsDefinition.getPackageName().get());
        nativeDistributions.setPackageVersion(nativeDistributionsDefinition.getPackageVersion().get());
        nativeDistributions.setDescription(nativeDistributionsDefinition.getDescription().get());
        nativeDistributions.setVendor(nativeDistributionsDefinition.getVendor().get());
        nativeDistributions.setCopyright(nativeDistributionsDefinition.getCopyrightYear().map(y -> "© " + y + " the original author or authors.").get());
        nativeDistributions.getAppResourcesRootDir().set(nativeDistributionsDefinition.getAppResourcesRootDir().get());

        nativeDistributions.setModules(new ArrayList<>(nativeDistributionsDefinition.getModules().get()));
    
        wireLinux(nativeDistributionsDefinition.getLinux(), nativeDistributions.getLinux());
        wireMacOS(nativeDistributionsDefinition.getMacOS(), nativeDistributions.getMacOS());
        wireWindows(nativeDistributionsDefinition.getWindows(), nativeDistributions.getWindows());
    }

    private static void wireWindows(Windows windowsDefinition, WindowsPlatformSettings windows) {
        windows.setMenu(windowsDefinition.getMenu().get());
        windows.setMenuGroup(windowsDefinition.getMenuGroup().get());
        windows.setPerUserInstall(windowsDefinition.getPerUserInstall().get());
        windows.setUpgradeUuid(windowsDefinition.getUpgradeUuid().get().trim());
        windows.getIconFile().set(windowsDefinition.getIconFile());
    }

    private static void wireMacOS(MacOS macOSDefinition, JvmMacOSPlatformSettings macOS) {
        macOS.setAppStore(macOSDefinition.getAppStore().get());
        macOS.setBundleID(macOSDefinition.getBundleID().get());
        macOS.setDockName(macOSDefinition.getDockName().get());
        macOS.getIconFile().set(macOSDefinition.getIconFile());
    }

    private static void wireLinux(Linux linuxDefinition, LinuxPlatformSettings linux) {
        linux.getIconFile().set(linuxDefinition.getIconFile());
    }
}
