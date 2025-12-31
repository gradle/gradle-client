package org.gradle.client.projectfeatures.compose;

import org.gradle.api.tasks.Nested;

public interface BuildType {
    @Nested
    Proguard getProguard();
}
