package org.gradle.client.projectfeatures.compose;

import org.gradle.features.binding.BuildModel;
import org.jetbrains.compose.ComposeExtension;

public interface ComposeBuildModel extends BuildModel {

    ComposeExtension getComposeExtension();

}
