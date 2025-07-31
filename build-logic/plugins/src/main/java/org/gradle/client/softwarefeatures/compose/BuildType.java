package org.gradle.client.softwarefeatures.compose;

import org.gradle.api.Action;
import org.gradle.api.tasks.Nested;
import org.gradle.declarative.dsl.model.annotations.Configuring;

public interface BuildType {
    @Nested
    Proguard getProguard();

    @Configuring
    default void proguard(Action<? super Proguard> action) {
        action.execute(getProguard());
    }
}
