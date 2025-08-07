package org.gradle.client.softwarefeatures.compose;

import org.gradle.api.Action;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Nested;
import org.gradle.declarative.dsl.model.annotations.Configuring;
import org.gradle.declarative.dsl.model.annotations.Restricted;

public interface NativeDistributions {
    @Restricted
    ListProperty<String> getTargetFormats();

    @Restricted
    Property<String> getPackageName();

    @Restricted
    Property<String> getPackageVersion();

    @Restricted
    Property<String> getDescription();

    @Restricted
    Property<String> getVendor();

    @Restricted
    Property<String> getCopyrightYear();

    @Restricted
    DirectoryProperty getAppResourcesRootDir();

    @Restricted
    ListProperty<String> getModules();

    @Nested
    Linux getLinux();

    @Configuring
    default void linux(Action<? super Linux> action) {
        action.execute(getLinux());
    }

    @Nested
    MacOS getMacOS();

    @Configuring
    default void macOS(Action<? super MacOS> action) {
        action.execute(getMacOS());
    }

    @Nested
    Windows getWindows();

    @Configuring
    default void windows(Action<? super Windows> action) {
        action.execute(getWindows());
    }
}
