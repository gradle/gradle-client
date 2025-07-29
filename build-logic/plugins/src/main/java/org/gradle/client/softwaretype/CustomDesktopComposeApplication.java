package org.gradle.client.softwaretype;

import org.gradle.api.Action;
import org.gradle.api.experimental.kmp.KmpApplication;
import org.gradle.api.internal.plugins.HasBuildModel;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Nested;
import org.gradle.client.softwaretype.compose.Compose;
import org.gradle.client.softwaretype.detekt.Detekt;
import org.gradle.client.softwaretype.sqldelight.SqlDelight;
import org.gradle.declarative.dsl.model.annotations.Configuring;
import org.gradle.declarative.dsl.model.annotations.Restricted;
import org.jspecify.annotations.NonNull;

public interface CustomDesktopComposeApplication extends HasBuildModel<@NonNull CustomDesktopComposeApplicationBuildModel> {
    @Restricted
    Property<String> getGroup();

    @Restricted
    Property<String> getVersion();

    @Nested
    KmpApplication getKotlinApplication();

    @Configuring
    default void kotlinApplication(Action<? super KmpApplication> action) {
        action.execute(getKotlinApplication());
    }
}
