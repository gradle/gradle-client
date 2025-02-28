package org.gradle.client.softwaretype.detekt;

import io.gitlab.arturbosch.detekt.extensions.DetektExtension;
import org.gradle.api.Project;
import org.gradle.client.softwaretype.CustomDesktopComposeApplication;

public final class DetektSupport {
    private DetektSupport() { /* not instantiable */ }

    public static void wireDetekt(Project project, CustomDesktopComposeApplication projectDefinition) {
        project.getPluginManager().apply("io.gitlab.arturbosch.detekt");

        project.afterEvaluate(p -> {
            Detekt detektDefinition = projectDefinition.getDetekt();
            DetektExtension detekt = project.getExtensions().findByType(DetektExtension.class);
            assert detekt != null;
            detekt.getSource().from(detektDefinition.getSource());
            detekt.getConfig().from(detektDefinition.getConfig());

            // This is not a property, need to wire this in afterEvaluate, so might as well wait to wire the rest of detekt with it
            detekt.setParallel(detektDefinition.getParallel().get());
        });
    }
}
