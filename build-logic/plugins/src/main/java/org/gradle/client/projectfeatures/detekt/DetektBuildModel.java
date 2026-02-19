package org.gradle.client.projectfeatures.detekt;

import io.gitlab.arturbosch.detekt.extensions.DetektExtension;
import org.gradle.features.binding.BuildModel;

public interface DetektBuildModel extends BuildModel {

    DetektExtension getDetektExtension();

}
