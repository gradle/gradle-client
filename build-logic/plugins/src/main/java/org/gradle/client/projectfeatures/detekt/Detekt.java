package org.gradle.client.projectfeatures.detekt;

import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.internal.plugins.Definition;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

public interface Detekt extends Definition<DetektBuildModel> {
    ListProperty<Directory> getSource();

    ListProperty<RegularFile> getConfig();

    Property<Boolean> getParallel();
}
