package org.gradle.client.projectfeatures.sqldelight;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.internal.plugins.Definition;
import org.gradle.api.provider.Property;

public interface SqlDelight extends Definition<SqlDelightBuildModel> {
    Property<String> getVersion();

    NamedDomainObjectContainer<Database> getDatabases();
}
