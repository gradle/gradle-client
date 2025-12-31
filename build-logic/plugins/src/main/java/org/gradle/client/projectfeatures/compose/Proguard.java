package org.gradle.client.projectfeatures.compose;

import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

public interface Proguard {
    Property<Boolean> getOptimize();

    Property<Boolean> getObfuscate();

    ListProperty<RegularFile> getConfigurationFiles();

    Property<String> getVersion();
}
