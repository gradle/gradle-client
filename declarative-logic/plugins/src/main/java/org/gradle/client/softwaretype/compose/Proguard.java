package org.gradle.client.softwaretype.compose;

import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.declarative.dsl.model.annotations.Restricted;

public interface Proguard {
    @Restricted
    Property<Boolean> getOptimize();

    @Restricted
    Property<Boolean> getObfuscate();

    @Restricted
    ListProperty<RegularFile> getConfigurationFiles();
}
