package org.gradle.client.softwaretype.sqldelight;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.internal.plugins.HasBuildModel;
import org.gradle.declarative.dsl.model.annotations.Restricted;
import org.jspecify.annotations.NonNull;

public interface SqlDelight extends HasBuildModel<@NonNull SqlDelightBuildModel> {
    NamedDomainObjectContainer<Database> getDatabases();
}
