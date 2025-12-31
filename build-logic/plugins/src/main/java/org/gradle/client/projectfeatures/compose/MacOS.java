package org.gradle.client.projectfeatures.compose;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

public interface MacOS {
    RegularFileProperty getIconFile();

    Property<Boolean> getAppStore();

    Property<String> getBundleID();

    Property<String> getDockName();
}
