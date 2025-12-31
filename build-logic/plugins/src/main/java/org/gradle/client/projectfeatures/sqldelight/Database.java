package org.gradle.client.projectfeatures.sqldelight;

import org.gradle.api.Named;
import org.gradle.api.provider.Property;

public interface Database extends Named {
    Property<String> getPackageName();

    Property<Boolean> getVerifyDefinitions();

    Property<Boolean> getVerifyMigrations();

    Property<Boolean> getDeriveSchemaFromMigrations();

    Property<Boolean> getGenerateAsync();
}
