package org.gradle.client.projectfeatures.sqldelight;

import org.gradle.api.Named;
import org.gradle.api.provider.Property;
import org.gradle.declarative.dsl.model.annotations.Restricted;

public interface Database extends Named {
    @Restricted
    Property<String> getPackageName();

    @Restricted
    Property<Boolean> getVerifyDefinitions();

    @Restricted
    Property<Boolean> getVerifyMigrations();

    @Restricted
    Property<Boolean> getDeriveSchemaFromMigrations();

    @Restricted
    Property<Boolean> getGenerateAsync();
}
