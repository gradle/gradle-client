package org.gradle.client.softwaretype;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.plugins.software.SoftwareType;

import static org.gradle.api.experimental.kmp.StandaloneKmpApplicationPlugin.PluginWiring.wireKMPApplication;
import static org.gradle.client.softwaretype.compose.ComposeSupport.wireCompose;
import static org.gradle.client.softwaretype.detekt.DetektSupport.wireDetekt;
import static org.gradle.client.softwaretype.sqldelight.SqlDelightSupport.wireSqlDelight;

@SuppressWarnings("UnstableApiUsage")
public abstract class CustomDesktopComposeApplicationPlugin implements Plugin<Project> {
    public static final String DESKTOP_COMPOSE_APP = "desktopComposeApp";

    @SoftwareType(name = DESKTOP_COMPOSE_APP, modelPublicType = CustomDesktopComposeApplication.class)
    public abstract CustomDesktopComposeApplication getDesktopComposeApp();

    @Override
    public void apply(Project project) {
        CustomDesktopComposeApplication projectDefinition = getDesktopComposeApp();

        wireKMPApplication(project, projectDefinition.getKotlinApplication());
        project.getPluginManager().apply("org.jetbrains.kotlin.plugin.serialization");

        wireProjectVersionInfo(project, projectDefinition);

        wireDetekt(project, projectDefinition);
        wireSqlDelight(project, projectDefinition);
        wireCompose(project, projectDefinition);
    }

    private static void wireProjectVersionInfo(Project project, CustomDesktopComposeApplication dslModel) {
        project.afterEvaluate(p -> {
            project.setGroup(dslModel.getGroup().get());
            project.setVersion(dslModel.getVersion().get());
        });
    }
}
