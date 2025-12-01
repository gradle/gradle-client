package org.gradle.client.projectfeatures.detekt;

import io.gitlab.arturbosch.detekt.extensions.DetektExtension;
import org.gradle.api.internal.plugins.BuildModel;

public interface DetektBuildModel extends BuildModel {

    DetektExtension getDetektExtension();

}
