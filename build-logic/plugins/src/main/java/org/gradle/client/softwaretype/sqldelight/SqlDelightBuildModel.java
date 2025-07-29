package org.gradle.client.softwaretype.sqldelight;

import app.cash.sqldelight.gradle.SqlDelightExtension;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.internal.plugins.BuildModel;
import org.gradle.api.provider.ProviderFactory;

import javax.inject.Inject;

public interface SqlDelightBuildModel extends BuildModel {

    SqlDelightExtension getSqlDelightExtension();

}
