package org.gradle.client.softwarefeatures.detekt;

import io.gitlab.arturbosch.detekt.extensions.DetektExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.experimental.kmp.KotlinMultiplatformBuildModel;
import org.gradle.api.internal.plugins.BindsProjectFeature;
import org.gradle.api.internal.plugins.ProjectFeatureBindingBuilder;
import org.gradle.api.internal.plugins.ProjectFeatureBinding;

import static org.gradle.api.internal.plugins.ProjectFeatureBindingBuilder.bindingToTargetBuildModel;

@SuppressWarnings("UnstableApiUsage")
@BindsProjectFeature(DetektProjectFeaturePlugin.Binding.class)
abstract public class DetektProjectFeaturePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {

    }

    static class Binding implements ProjectFeatureBinding {
        @Override
        public void bind(ProjectFeatureBindingBuilder builder) {
            builder.bindProjectFeature("detekt", bindingToTargetBuildModel(Detekt.class, KotlinMultiplatformBuildModel.class),
                    (context, definition, buildModel, parent) -> {

                        Project project = context.getProject();
                        project.getPluginManager().apply("io.gitlab.arturbosch.detekt");
                        ((DefaultDetektBuildModel)buildModel).setDetektExtension(project.getExtensions().findByType(DetektExtension.class));

                        buildModel.getDetektExtension().getSource().from(definition.getSource());
                        buildModel.getDetektExtension().getConfig().from(definition.getConfig());

                        project.afterEvaluate(p -> {
                            // This is not a property, need to wire this in afterEvaluate
                            buildModel.getDetektExtension().setParallel(definition.getParallel().get());
                        });
                    }
            ).withBuildModelImplementationType(DefaultDetektBuildModel.class);
        }
    }
}
