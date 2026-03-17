package org.gradle.client.projectfeatures.compose;

import org.gradle.api.provider.Property;
import org.gradle.features.binding.BuildModel;
import org.jetbrains.compose.ComposeExtension;

public interface ComposeBuildModel extends BuildModel {

    Property<String> getPackageVersion();

    ComposeExtension getComposeExtension();

}
