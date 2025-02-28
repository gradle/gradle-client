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

    public static void wireCompose(Project project, CustomDesktopComposeApplication dslModel) {
        project.getPluginManager().apply("org.jetbrains.kotlin.plugin.compose");

        project.afterEvaluate(p -> {
            project.getPluginManager().apply("org.jetbrains.compose");
            ComposeExtension compose = project.getExtensions().getByType(ComposeExtension.class);
            DesktopExtension desktop = compose.getExtensions().getByType(DesktopExtension.class);
            JvmApplication application = desktop.getApplication();
            JvmApplicationDistributions nativeDistributions = application.getNativeDistributions();

            Compose composeModel = dslModel.getCompose();
            application.setMainClass(composeModel.getMainClass().get());
            composeModel.getJvmArgs().forEach(jvmArg -> application.getJvmArgs().add(jvmArg.getName() + jvmArg.getValue().get()));

            wireBuildTypes(composeModel, application.getBuildTypes());

            wireNativeDistribution(composeModel.getNativeDistributions(), nativeDistributions);

            // Need to set the main class for the JVM run task in the KMP model from the one nested in the Compose model
            KotlinMultiplatformExtension kotlin = project.getExtensions().getByType(KotlinMultiplatformExtension.class);
            kotlin.jvm("jvm" , jvm -> {
                jvm.mainRun(kotlinJvmRunDsl -> {
                    kotlinJvmRunDsl.getMainClass().set(composeModel.getMainClass());
                    return Unit.INSTANCE;
                });
            });
        });
    }

    private static void wireBuildTypes(Compose composeModel, JvmApplicationBuildTypes buildTypes) {
        wireBuildType(composeModel.getBuildTypes().getRelease(), buildTypes.getRelease());
    }

    private static void wireBuildType(BuildType buildTypeModel, JvmApplicationBuildType buildType) {
        ProguardSettings proguard = buildType.getProguard();
        Proguard proguardModel = buildTypeModel.getProguard();
        proguard.getOptimize().set(proguardModel.getOptimize());
        proguard.getObfuscate().set(proguardModel.getObfuscate());

        proguardModel.getConfigurationFiles().get().forEach(f -> proguard.getConfigurationFiles().from(f));
    }

    private static void wireNativeDistribution(NativeDistributions nativeDistributionsModel, JvmApplicationDistributions nativeDistributions) {
        nativeDistributions.setTargetFormats(EnumSet.copyOf(nativeDistributionsModel.getTargetFormats().get().stream().map(TargetFormat::valueOf).collect(Collectors.toList())));

        nativeDistributions.setPackageName(nativeDistributionsModel.getPackageName().get());
        nativeDistributions.setPackageVersion(nativeDistributionsModel.getPackageVersion().get());
        nativeDistributions.setDescription(nativeDistributionsModel.getDescription().get());
        nativeDistributions.setVendor(nativeDistributionsModel.getVendor().get());
        nativeDistributions.setCopyright(nativeDistributionsModel.getCopyrightYear().map(y -> "© " + y + " the original author or authors.").get());
        nativeDistributions.getAppResourcesRootDir().set(nativeDistributionsModel.getAppResourcesRootDir().get());

        nativeDistributions.setModules(new ArrayList<>(nativeDistributionsModel.getModules().get()));
    
        wireLinux(nativeDistributionsModel.getLinux(), nativeDistributions.getLinux());
        wireMacOS(nativeDistributionsModel.getMacOS(), nativeDistributions.getMacOS());
        wireWindows(nativeDistributionsModel.getWindows(), nativeDistributions.getWindows());
    }

    private static void wireWindows(Windows windowsModel, WindowsPlatformSettings windows) {
        windows.setMenu(windowsModel.getMenu().get());
        windows.setMenuGroup(windowsModel.getMenuGroup().get());
        windows.setPerUserInstall(windowsModel.getPerUserInstall().get());
        windows.setUpgradeUuid(windowsModel.getUpgradeUuid().get().trim());
        windows.getIconFile().set(windowsModel.getIconFile());
    }

    private static void wireMacOS(MacOS macOSModel, JvmMacOSPlatformSettings macOS) {
        macOS.setAppStore(macOSModel.getAppStore().get());
        macOS.setBundleID(macOSModel.getBundleID().get());
        macOS.setDockName(macOSModel.getDockName().get());
        macOS.getIconFile().set(macOSModel.getIconFile());
    }

    private static void wireLinux(Linux linuxModel, LinuxPlatformSettings linux) {
        linux.getIconFile().set(linuxModel.getIconFile());
    }
}
