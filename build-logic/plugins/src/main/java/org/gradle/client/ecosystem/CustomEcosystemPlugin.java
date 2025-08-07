package org.gradle.client.ecosystem;

import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.plugins.software.RegistersSoftwareTypes;
import org.gradle.client.softwarefeatures.compose.ComposeSoftwareFeaturePlugin;
import org.gradle.client.softwarefeatures.detekt.DetektSoftwareFeaturePlugin;
import org.gradle.client.softwarefeatures.sqldelight.SqlDelightSoftwareFeaturePlugin;

@SuppressWarnings("UnstableApiUsage")
@RegistersSoftwareTypes({
        ComposeSoftwareFeaturePlugin.class,
        DetektSoftwareFeaturePlugin.class,
        SqlDelightSoftwareFeaturePlugin.class
})
public abstract class CustomEcosystemPlugin implements Plugin<Settings> {
    @Override
    public void apply(Settings settings) {}
}
