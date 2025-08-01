package org.gradle.client.softwarefeatures.compose;

import kotlin.Unit;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.experimental.kmp.KotlinMultiplatformBuildModel;
import org.gradle.api.internal.plugins.BindsSoftwareFeature;
import org.gradle.api.internal.plugins.SoftwareFeatureBindingBuilder;
import org.gradle.api.internal.plugins.SoftwareFeatureBindingRegistration;
import org.jetbrains.compose.ComposeExtension;
import org.jetbrains.compose.desktop.DesktopExtension;
import org.jetbrains.compose.desktop.application.dsl.*;
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.stream.Collectors;

import static org.gradle.api.internal.plugins.SoftwareFeatureBindingBuilder.bindingToTargetBuildModel;

@SuppressWarnings("UnstableApiUsage")
@BindsSoftwareFeature(ComposeSoftwareFeaturePlugin.Binding.class)
abstract public class ComposeSoftwareFeaturePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {

    }

    static class Binding implements SoftwareFeatureBindingRegistration {
        @Override
        public void register(SoftwareFeatureBindingBuilder builder) {
            builder.bindSoftwareFeature("compose", bindingToTargetBuildModel(Compose.class, KotlinMultiplatformBuildModel.class),
                    (context, definition, buildModel, parent) -> {
                        Project project = context.getProject();
                        project.getPluginManager().apply("org.jetbrains.kotlin.plugin.serialization");
                        project.getPluginManager().apply("org.jetbrains.kotlin.plugin.compose");

                        definition.getNativeDistributions().getPackageVersion().convention(project.provider(() -> project.getVersion().toString()));

                        project.getPluginManager().apply("org.jetbrains.compose");
                        ((DefaultComposeBuildModel)buildModel).setComposeExtension(project.getExtensions().getByType(ComposeExtension.class));
                        DesktopExtension desktop =  buildModel.getComposeExtension().getExtensions().getByType(DesktopExtension.class);
                        JvmApplication application = desktop.getApplication();
                        JvmApplicationDistributions nativeDistributions = application.getNativeDistributions();

                        // Many of the underlying extension elements are not provider-aware, so we have to do
                        // this in an afterEvaluate block
                        project.afterEvaluate(p -> {

                            wireNativeDistribution(definition.getNativeDistributions(), nativeDistributions);
                            application.setMainClass(definition.getMainClass().get());
                            definition.getJvmArgs().forEach(jvmArg -> application.getJvmArgs().add(jvmArg.getName() + jvmArg.getValue().get()));

                            wireBuildTypes(definition, application.getBuildTypes());

                            // Need to set the main class for the JVM run task in the KMP model from the one nested in the Compose model
                            KotlinMultiplatformExtension kotlin = project.getExtensions().getByType(KotlinMultiplatformExtension.class);
                            kotlin.jvm("jvm" , jvm -> {
                                jvm.mainRun(kotlinJvmRunDsl -> {
                                    kotlinJvmRunDsl.getMainClass().set(definition.getMainClass());
                                    return Unit.INSTANCE;
                                });
                            });
                        });
                    }).withBuildModelImplementationType(DefaultComposeBuildModel.class);
        }

        private static void wireBuildTypes(Compose composeBuildModel, JvmApplicationBuildTypes buildTypes) {
            wireBuildType(composeBuildModel.getBuildTypes().getRelease(), buildTypes.getRelease());
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
}
