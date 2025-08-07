package org.gradle.client.softwarefeatures.compose;

import org.gradle.api.internal.plugins.BuildModel;
import org.jetbrains.compose.ComposeExtension;

public interface ComposeBuildModel extends BuildModel {

    ComposeExtension getComposeExtension();

}
