package org.gradle.client.softwaretype.compose;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.declarative.dsl.model.annotations.Restricted;

@Restricted
public interface MacOS {
    @Restricted
    RegularFileProperty getIconFile();

    @Restricted
    Property<Boolean> getAppStore();

    @Restricted
    Property<String> getBundleID();

    @Restricted
    Property<String> getDockName();
}
