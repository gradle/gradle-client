package org.gradle.client.softwaretype.detekt;

import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.internal.plugins.HasBuildModel;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.declarative.dsl.model.annotations.Restricted;
import org.jspecify.annotations.NonNull;

public interface Detekt extends HasBuildModel<@NonNull DetektBuildModel> {
    @Restricted
    ListProperty<Directory> getSource();

    @Restricted
    ListProperty<RegularFile> getConfig();

    @Restricted
    Property<Boolean> getParallel();
}
