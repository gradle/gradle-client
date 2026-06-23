package org.gradle.client.projectfeatures.detekt;

import io.gitlab.arturbosch.detekt.extensions.DetektExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.experimental.kmp.KotlinMultiplatformBuildModel;
import org.gradle.api.model.ObjectFactory;
import org.gradle.features.annotations.BindsProjectFeature;
import org.gradle.features.binding.*;
import org.gradle.api.plugins.PluginManager;

import javax.inject.Inject;

@SuppressWarnings("UnstableApiUsage")
@BindsProjectFeature(DetektProjectFeaturePlugin.Binding.class)
abstract public class DetektProjectFeaturePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {

    }

    static class Binding implements ProjectFeatureBinding {
        @Override
        public void bind(ProjectFeatureBindingBuilder builder) {
            builder.bindProjectFeatureToBuildModel("detekt", Detekt.class, KotlinMultiplatformBuildModel.class, ApplyAction.class)
            .withUnsafeApplyAction()
            .withBuildModelImplementationType(DefaultDetektBuildModel.class);
        }

        static abstract class ApplyAction implements ProjectFeatureApplyAction<Detekt, DetektBuildModel, Definition<KotlinMultiplatformBuildModel>> {

            @Inject
            public ApplyAction() {
            }

            @Inject
            public abstract ObjectFactory getObjectFactory();

            @Override
            public void apply(ProjectFeatureApplicationContext context, Detekt definition, DetektBuildModel buildModel, Definition<KotlinMultiplatformBuildModel> kotlinMultiplatformBuildModelDefinition) {
                Services services = getObjectFactory().newInstance(Services.class);

                services.getPluginManager().apply("io.gitlab.arturbosch.detekt");
                ((DefaultDetektBuildModel)buildModel).setDetektExtension(services.getProject().getExtensions().getByType(DetektExtension.class));

                buildModel.getDetektExtension().getSource().from(definition.getSource());
                buildModel.getDetektExtension().getConfig().from(definition.getConfig());

                services.getProject().afterEvaluate(p -> {
                    // This is not a property, need to wire this in afterEvaluate
                    buildModel.getDetektExtension().setParallel(definition.getParallel().get());
                });
            }
        }

        interface Services {
            @Inject
            PluginManager getPluginManager();

            @Inject
            Project getProject();
        }
    }
}
