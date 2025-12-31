package org.gradle.client.projectfeatures.compose;

import org.gradle.api.Named;
import org.gradle.api.provider.Property;

public interface Module extends Named {
    Property<String> getValue();
}
