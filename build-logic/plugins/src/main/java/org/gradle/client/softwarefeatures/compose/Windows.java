package org.gradle.client.softwarefeatures.compose;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.declarative.dsl.model.annotations.Restricted;

public interface Windows {
    @Restricted
    RegularFileProperty getIconFile();

    @Restricted
    Property<Boolean> getMenu();

    @Restricted
    Property<String> getMenuGroup();

    @Restricted
    Property<Boolean> getPerUserInstall();

    @Restricted
    Property<String> getUpgradeUuid();
}
