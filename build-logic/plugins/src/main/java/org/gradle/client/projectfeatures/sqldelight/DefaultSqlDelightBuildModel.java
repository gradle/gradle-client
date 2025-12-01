package org.gradle.client.projectfeatures.sqldelight;

import app.cash.sqldelight.gradle.SqlDelightExtension;

public class DefaultSqlDelightBuildModel implements SqlDelightBuildModel {
    private SqlDelightExtension sqlDelightExtension;

    @Override
    public SqlDelightExtension getSqlDelightExtension() {
        return sqlDelightExtension;
    }

    public void setSqlDelightExtension(SqlDelightExtension sqlDelightExtension) {
        this.sqlDelightExtension = sqlDelightExtension;
    }
}
