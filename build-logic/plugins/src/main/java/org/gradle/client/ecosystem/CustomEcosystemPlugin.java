package org.gradle.client.ecosystem;

import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.plugins.software.RegistersSoftwareTypes;
import org.gradle.client.softwarefeatures.compose.ComposeProjectFeaturePlugin;
import org.gradle.client.softwarefeatures.detekt.DetektProjectFeaturePlugin;
import org.gradle.client.softwarefeatures.sqldelight.SqlDelightProjectFeaturePlugin;

@SuppressWarnings("UnstableApiUsage")
@RegistersSoftwareTypes({
        ComposeProjectFeaturePlugin.class,
        DetektProjectFeaturePlugin.class,
        SqlDelightProjectFeaturePlugin.class
})
public abstract class CustomEcosystemPlugin implements Plugin<Settings> {
    @Override
    public void apply(Settings settings) {}
}
