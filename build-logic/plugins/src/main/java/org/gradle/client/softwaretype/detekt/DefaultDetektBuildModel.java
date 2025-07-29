package org.gradle.client.softwaretype.detekt;

import io.gitlab.arturbosch.detekt.extensions.DetektExtension;

public class DefaultDetektBuildModel implements DetektBuildModel {
    private DetektExtension detektExtension;

    @Override
    public DetektExtension getDetektExtension() {
        return detektExtension;
    }

    public void setDetektExtension(DetektExtension detektExtension) {
        this.detektExtension = detektExtension;
    }
}
