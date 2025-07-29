package org.gradle.client.softwaretype;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.plugins.BindsSoftwareType;
import org.gradle.api.internal.plugins.SoftwareTypeBindingBuilder;
import org.gradle.api.internal.plugins.SoftwareTypeBindingRegistration;

import static org.gradle.api.experimental.kmp.StandaloneKmpApplicationPlugin.PluginWiring.wireKMPApplication;

@SuppressWarnings("UnstableApiUsage")
@BindsSoftwareType(CustomDesktopComposeApplicationPlugin.Binding.class)
public abstract class CustomDesktopComposeApplicationPlugin implements Plugin<Project> {
    public static final String DESKTOP_COMPOSE_APP = "desktopComposeApp";

    @Override
    public void apply(Project project) {
    }

    static class Binding implements SoftwareTypeBindingRegistration {

        @Override
        public void register(SoftwareTypeBindingBuilder builder) {
            builder.bindSoftwareType(DESKTOP_COMPOSE_APP, CustomDesktopComposeApplication.class,
                    (context, definition, buildModel) -> {
                        buildModel.copyFrom(definition);
                        Project project = context.getProject();
                        // This really should _be_ a kmp application, with this custom type just going away
                        wireKMPApplication(project, buildModel.getKotlinApplication());
                        project.getPluginManager().apply("org.jetbrains.kotlin.plugin.serialization");
                        project.afterEvaluate(p -> {
                            project.setGroup(buildModel.getGroup().get());
                            project.setVersion(buildModel.getVersion().get());
                        });
                    }
            );
        }
    }
}
