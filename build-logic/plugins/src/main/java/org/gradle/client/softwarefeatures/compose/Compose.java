package org.gradle.client.softwarefeatures.compose;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.internal.plugins.HasBuildModel;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Nested;
import org.gradle.declarative.dsl.model.annotations.Configuring;
import org.gradle.declarative.dsl.model.annotations.Restricted;
import org.jspecify.annotations.NonNull;

public interface Compose extends HasBuildModel<@NonNull ComposeBuildModel> {
    @Restricted
    Property<String> getMainClass();

    NamedDomainObjectContainer<JvmArg> getJvmArgs();

    @Nested
    BuildTypes getBuildTypes();

    @Configuring
    default void buildTypes(Action<? super BuildTypes> action) {
        action.execute(getBuildTypes());
    }

    @Nested
    NativeDistributions getNativeDistributions();

    @Configuring
    default void nativeDistributions(Action<? super NativeDistributions> action) {
        action.execute(getNativeDistributions());
    }
}
