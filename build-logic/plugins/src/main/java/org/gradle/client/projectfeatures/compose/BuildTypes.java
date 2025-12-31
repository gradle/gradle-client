package org.gradle.client.projectfeatures.compose;

import org.gradle.api.tasks.Nested;

public interface BuildTypes {
    @Nested
    BuildType getRelease();
}
