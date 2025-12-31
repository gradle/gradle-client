package org.gradle.client.projectfeatures.compose;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.internal.plugins.Definition;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Nested;

public interface Compose extends Definition<ComposeBuildModel> {
    Property<String> getMainClass();

    NamedDomainObjectContainer<JvmArg> getJvmArgs();

    @Nested
    BuildTypes getBuildTypes();

    @Nested
    NativeDistributions getNativeDistributions();
}
