package org.gradle.client.softwaretype.detekt;

import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.declarative.dsl.model.annotations.Restricted;

public interface Detekt {
    @Restricted
    ListProperty<Directory> getSource();

    @Restricted
    ListProperty<RegularFile> getConfig();

    @Restricted
    Property<Boolean> getParallel();
}
