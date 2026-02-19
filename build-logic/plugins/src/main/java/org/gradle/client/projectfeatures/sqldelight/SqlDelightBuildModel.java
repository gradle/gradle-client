package org.gradle.client.projectfeatures.sqldelight;

import app.cash.sqldelight.gradle.SqlDelightExtension;
import org.gradle.features.binding.BuildModel;

public interface SqlDelightBuildModel extends BuildModel {

    SqlDelightExtension getSqlDelightExtension();

}
