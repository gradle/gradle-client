package org.gradle.client.projectfeatures.sqldelight;

import app.cash.sqldelight.gradle.SqlDelightExtension;
import org.gradle.api.provider.Property;
import org.gradle.features.binding.BuildModel;

public interface SqlDelightBuildModel extends BuildModel {
    Property<String> getVersion();

    SqlDelightExtension getSqlDelightExtension();
}
