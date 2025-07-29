package org.gradle.client.softwaretype;

import org.gradle.api.experimental.kmp.KmpApplication;
import org.gradle.api.internal.plugins.BuildModel;
import org.gradle.api.provider.Property;

public abstract class CustomDesktopComposeApplicationBuildModel implements BuildModel {
    private CustomDesktopComposeApplication delegate;

    public Property<String> getGroup() {
        return delegate.getGroup();
    }

    public Property<String> getVersion() {
        return delegate.getVersion();
    }

    public KmpApplication getKotlinApplication() {
        return delegate.getKotlinApplication();
    }

    void copyFrom(CustomDesktopComposeApplication definition) {
        this.delegate = definition;
    }
}
