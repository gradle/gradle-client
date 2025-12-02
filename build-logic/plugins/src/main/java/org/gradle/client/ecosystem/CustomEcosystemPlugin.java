package org.gradle.client.ecosystem;

import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.plugins.software.RegistersProjectFeatures;
import org.gradle.client.projectfeatures.compose.ComposeProjectFeaturePlugin;
import org.gradle.client.projectfeatures.detekt.DetektProjectFeaturePlugin;
import org.gradle.client.projectfeatures.sqldelight.SqlDelightProjectFeaturePlugin;

@SuppressWarnings("UnstableApiUsage")
@RegistersProjectFeatures({
        ComposeProjectFeaturePlugin.class,
        DetektProjectFeaturePlugin.class,
        SqlDelightProjectFeaturePlugin.class
})
public abstract class CustomEcosystemPlugin implements Plugin<Settings> {
    @Override
    public void apply(Settings settings) {}
}
