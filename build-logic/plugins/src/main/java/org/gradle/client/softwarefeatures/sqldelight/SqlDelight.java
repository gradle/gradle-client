package org.gradle.client.softwarefeatures.sqldelight;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.internal.plugins.HasBuildModel;
import org.gradle.api.provider.Property;
import org.jspecify.annotations.NonNull;

public interface SqlDelight extends HasBuildModel<@NonNull SqlDelightBuildModel> {
    Property<String> getVersion();

    NamedDomainObjectContainer<Database> getDatabases();
}
