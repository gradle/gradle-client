package org.gradle.client.softwaretype.compose;

import org.gradle.api.Named;
import org.gradle.api.provider.Property;
import org.gradle.declarative.dsl.model.annotations.Restricted;

public interface Module extends Named {
    @Restricted
    Property<String> getValue();
}
