package org.gradle.client.projectfeatures.sqldelight;

import app.cash.sqldelight.gradle.SqlDelightExtension;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.DependencyScopeConfiguration;
import org.gradle.api.experimental.kmp.KotlinMultiplatformBuildModel;
import org.gradle.api.internal.plugins.BindsProjectFeature;
import org.gradle.api.internal.plugins.ProjectFeatureBindingBuilder;
import org.gradle.api.internal.plugins.ProjectFeatureBinding;

import static java.util.Arrays.stream;
import static org.gradle.api.internal.plugins.ProjectFeatureBindingBuilder.bindingToTargetBuildModel;

@SuppressWarnings("UnstableApiUsage")
@BindsProjectFeature(SqlDelightProjectFeaturePlugin.Binding.class)
abstract public class SqlDelightProjectFeaturePlugin implements Plugin<Project> {
    private static final String SQLDELIGHT_GROUP = "app.cash.sqldelight";
    private static final String[] SQLDELIGHT_DEPENDENCY_MODULES = new String[] {
            "runtime",
            "coroutines-extensions",
            "sqlite-driver"
    };
    @Override
    public void apply(Project project) {

    }

    static class Binding implements ProjectFeatureBinding {
        @Override
        public void bind(ProjectFeatureBindingBuilder builder) {
            builder.bindProjectFeatureToBuildModel("sqlDelight", SqlDelight.class, KotlinMultiplatformBuildModel.class,
                (context, definition, buildModel, parent) -> {
                    Project project = context.getProject();

                    definition.getVersion().convention("2.0.2");

                    KotlinMultiplatformBuildModel parentBuildModel = context.getBuildModel(parent);
                    parentBuildModel.getKotlinMultiplatformExtension().jvm(jvmTarget -> {
                        NamedDomainObjectProvider<DependencyScopeConfiguration> sqlDelightConfiguration = project.getConfigurations().dependencyScope("sqlDelightTool", conf -> {
                            stream(SQLDELIGHT_DEPENDENCY_MODULES).forEach(module ->
                                conf.getDependencies().addLater(definition.getVersion().map(version -> project.getDependencyFactory().create(SQLDELIGHT_GROUP + ":" +module + ":" + version)))
                            );
                        });

                        project.getConfigurations().named(jvmTarget.getCompilations().getByName("main").getDefaultSourceSet().getImplementationConfigurationName(), conf ->
                                conf.extendsFrom(sqlDelightConfiguration.get())
                        );
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
