package org.gradle.client.projectfeatures.compose;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

public interface Windows {
    RegularFileProperty getIconFile();

    Property<Boolean> getMenu();

    Property<String> getMenuGroup();

    Property<Boolean> getPerUserInstall();

    Property<String> getUpgradeUuid();
}
