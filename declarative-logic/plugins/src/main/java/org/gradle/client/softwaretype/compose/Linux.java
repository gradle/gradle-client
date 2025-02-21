package org.gradle.client.softwaretype.compose;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.declarative.dsl.model.annotations.Restricted;

public interface Linux {
    @Restricted
    RegularFileProperty getIconFile();
}
