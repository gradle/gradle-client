package org.gradle.client.softwarefeatures.sqldelight;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.internal.plugins.HasBuildModel;
import org.jspecify.annotations.NonNull;

public interface SqlDelight extends HasBuildModel<@NonNull SqlDelightBuildModel> {
    NamedDomainObjectContainer<Database> getDatabases();
}
