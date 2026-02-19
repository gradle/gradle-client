package org.gradle.client.projectfeatures.sqldelight;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.features.binding.Definition;
import org.gradle.api.provider.Property;

public interface SqlDelight extends Definition<SqlDelightBuildModel> {
    Property<String> getVersion();

    NamedDomainObjectContainer<Database> getDatabases();
}
