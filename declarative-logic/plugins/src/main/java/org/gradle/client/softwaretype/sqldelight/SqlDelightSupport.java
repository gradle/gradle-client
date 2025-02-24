package org.gradle.client.softwaretype.sqldelight;

import app.cash.sqldelight.gradle.SqlDelightExtension;
import org.gradle.api.Project;
import org.gradle.client.softwaretype.CustomDesktopComposeApplication;

public final class SqlDelightSupport {
    private SqlDelightSupport() { /* not instantiable */ }

    public static boolean needToWireSqlDelight(CustomDesktopComposeApplication dslModel) {
        return !dslModel.getSqlDelight().getDatabases().isEmpty();
    }

    @SuppressWarnings("CodeBlock2Expr")
    public static void wireSqlDelight(Project project, CustomDesktopComposeApplication dslModel) {
        /*
         * It's necessary to defer checking the NDOC in our extension for contents until after project evaluation.
         * If you move the check below outside of afterEvaluate, it fails.  Inside, it succeeds.
         * Without the afterEvaluate, the databases is seen as empty, and the plugin fails, with this warning:
         * https://github.com/plangrid/sqldelight/blob/917cb8e5ee437d37bfdbdcbb3fded09b683fe826/sqldelight-gradle-plugin/src/main/kotlin/app/cash/sqldelight/gradle/SqlDelightPlugin.kt#L112
         */
        project.afterEvaluate(p -> {
            if (needToWireSqlDelight(dslModel)) {
                project.getPluginManager().apply("app.cash.sqldelight");

                dslModel.getKotlinApplication().getTargets().jvm(jvmTarget -> {
                    jvmTarget.getDependencies().getImplementation().add("app.cash.sqldelight:runtime:2.0.2");
                    jvmTarget.getDependencies().getImplementation().add("app.cash.sqldelight:coroutines-extensions:2.0.2");
                    jvmTarget.getDependencies().getImplementation().add("app.cash.sqldelight:sqlite-driver:2.0.2");
                });

                SqlDelightExtension sqlDelight = project.getExtensions().getByType(SqlDelightExtension.class);
                dslModel.getSqlDelight().getDatabases().forEach(dslModelDatabase -> {
                    sqlDelight.getDatabases().create(dslModelDatabase.getName(), database -> {
                        database.getPackageName().set(dslModelDatabase.getPackageName());
                        database.getVerifyDefinitions().set(dslModelDatabase.getVerifyDefinitions());
                        database.getVerifyMigrations().set(dslModelDatabase.getVerifyMigrations());
                        database.getDeriveSchemaFromMigrations().set(dslModelDatabase.getDeriveSchemaFromMigrations());
                        database.getGenerateAsync().set(dslModelDatabase.getGenerateAsync());
                    });
                });
            }
        });
    }
}
