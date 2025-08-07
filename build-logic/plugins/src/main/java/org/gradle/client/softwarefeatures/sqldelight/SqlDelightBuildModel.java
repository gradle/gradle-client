package org.gradle.client.softwarefeatures.sqldelight;

import app.cash.sqldelight.gradle.SqlDelightExtension;
import org.gradle.api.internal.plugins.BuildModel;

public interface SqlDelightBuildModel extends BuildModel {

    SqlDelightExtension getSqlDelightExtension();

}
