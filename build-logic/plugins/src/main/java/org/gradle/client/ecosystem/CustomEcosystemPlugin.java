package org.gradle.client.ecosystem;

import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.plugins.software.RegistersSoftwareTypes;
import org.gradle.client.softwaretype.CustomDesktopComposeApplicationPlugin;
import org.gradle.client.softwaretype.compose.ComposeSoftwareFeaturePlugin;
import org.gradle.client.softwaretype.detekt.DetektSoftwareFeaturePlugin;
import org.gradle.client.softwaretype.sqldelight.SqlDelightSoftwareFeaturePlugin;

@SuppressWarnings("UnstableApiUsage")
@RegistersSoftwareTypes({
        CustomDesktopComposeApplicationPlugin.class,
        ComposeSoftwareFeaturePlugin.class,
        DetektSoftwareFeaturePlugin.class,
        SqlDelightSoftwareFeaturePlugin.class
})
public abstract class CustomEcosystemPlugin implements Plugin<Settings> {
    @Override
    public void apply(Settings settings) {}
}
