package org.gradle.client.softwaretype.sqldelight;

import app.cash.sqldelight.gradle.SqlDelightExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.plugins.BindsSoftwareFeature;
import org.gradle.api.internal.plugins.SoftwareFeatureBindingBuilder;
import org.gradle.api.internal.plugins.SoftwareFeatureBindingRegistration;
import org.gradle.client.softwaretype.CustomDesktopComposeApplicationBuildModel;

import static org.gradle.api.internal.plugins.SoftwareFeatureBindingBuilder.bindingToTargetBuildModel;

@SuppressWarnings("UnstableApiUsage")
@BindsSoftwareFeature(SqlDelightSoftwareFeaturePlugin.Binding.class)
abstract public class SqlDelightSoftwareFeaturePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {

    }

    static class Binding implements SoftwareFeatureBindingRegistration {
        @Override
        public void register(SoftwareFeatureBindingBuilder builder) {
            builder.bindSoftwareFeature("sqlDelight", bindingToTargetBuildModel(SqlDelight.class, CustomDesktopComposeApplicationBuildModel.class),
                (context, definition, buildModel, parent) -> {
                    Project project = context.getProject();

                    CustomDesktopComposeApplicationBuildModel parentBuildModel = context.getOrCreateModel(parent);
                    parentBuildModel.getKotlinApplication().getTargets().jvm(jvmTarget -> {
                        jvmTarget.getDependencies().getImplementation().add("app.cash.sqldelight:runtime:2.0.2");
                        jvmTarget.getDependencies().getImplementation().add("app.cash.sqldelight:coroutines-extensions:2.0.2");
                        jvmTarget.getDependencies().getImplementation().add("app.cash.sqldelight:sqlite-driver:2.0.2");
                    });

                    project.afterEvaluate(p -> {
                        // for some reason this only works in an afterEvaluate block.  I suspect it has something to do with
                        // much of the compose configuration being applied in an afterEvaluate.
                        project.getPluginManager().apply("app.cash.sqldelight");
                        ((DefaultSqlDelightBuildModel)buildModel).setSqlDelightExtension(project.getExtensions().getByType(SqlDelightExtension.class));

                        definition.getDatabases().forEach(featureDatabase -> {
                            buildModel.getSqlDelightExtension().getDatabases().create(featureDatabase.getName(), database -> {
                                database.getPackageName().set(featureDatabase.getPackageName());
                                database.getVerifyDefinitions().set(featureDatabase.getVerifyDefinitions());
                                database.getVerifyMigrations().set(featureDatabase.getVerifyMigrations());
                                database.getDeriveSchemaFromMigrations().set(featureDatabase.getDeriveSchemaFromMigrations());
                                database.getGenerateAsync().set(featureDatabase.getGenerateAsync());
                            });
                        });
                    });
                }).withBuildModelImplementationType(DefaultSqlDelightBuildModel.class);
        }
    }
}
