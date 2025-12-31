package org.gradle.client.projectfeatures.compose;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Nested;

public interface NativeDistributions {
    ListProperty<String> getTargetFormats();

    Property<String> getPackageName();

    Property<String> getPackageVersion();

    Property<String> getDescription();

    Property<String> getVendor();

    Property<String> getCopyrightYear();

    DirectoryProperty getAppResourcesRootDir();

    ListProperty<String> getModules();

    @Nested
    Linux getLinux();

    @Nested
    MacOS getMacOS();

    @Nested
    Windows getWindows();
}
