package org.gradle.client.projectfeatures.sqldelight;

import app.cash.sqldelight.gradle.SqlDelightDatabase;
import app.cash.sqldelight.gradle.SqlDelightExtension;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.DependencyScopeConfiguration;
import org.gradle.api.experimental.kmp.KotlinMultiplatformBuildModel;
import org.gradle.api.internal.plugins.BindsProjectFeature;
import org.gradle.api.internal.plugins.ProjectFeatureBindingBuilder;
import org.gradle.api.internal.plugins.ProjectFeatureBinding;
import org.gradle.api.provider.Provider;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

@SuppressWarnings("UnstableApiUsage")
@BindsProjectFeature(SqlDelightProjectFeaturePlugin.Binding.class)
abstract public class SqlDelightProjectFeaturePlugin implements Plugin<Project> {
    private static final String SQLDELIGHT_GROUP = "app.cash.sqldelight";
    private static final String[] SQLDELIGHT_DEPENDENCY_MODULES = new String[]{
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

                    definition.getVersion().convention("2.1.0");

                    KotlinMultiplatformBuildModel parentBuildModel = context.getBuildModel(parent);
                    parentBuildModel.getKotlinMultiplatformExtension().jvm(jvmTarget -> {
                        NamedDomainObjectProvider<DependencyScopeConfiguration> sqlDelightConfiguration = project.getConfigurations().dependencyScope("sqlDelightTool", conf -> {
                            stream(SQLDELIGHT_DEPENDENCY_MODULES).forEach(module ->
                                conf.getDependencies().addLater(definition.getVersion().map(version -> project.getDependencyFactory().create(SQLDELIGHT_GROUP + ":" + module + ":" + version)))
                            );
                        });

                        project.getConfigurations().named(jvmTarget.getCompilations().getByName("main").getDefaultSourceSet().getImplementationConfigurationName(), conf ->
                                conf.extendsFrom(sqlDelightConfiguration.get())
                        );
                    });

                    project.getPluginManager().apply("app.cash.sqldelight");
                    ((DefaultSqlDelightBuildModel) buildModel).setSqlDelightExtension(project.getExtensions().getByType(SqlDelightExtension.class));

                    // https://github.com/gradle/gradle/issues/28043
                    Provider<Set<Database>> featureDatabasesProvider = project.provider(() -> Collections.unmodifiableSet(definition.getDatabases()));

                    Provider<Set<SqlDelightDatabase>> sqlDelightDatabases = featureDatabasesProvider.map(featureDatabases -> featureDatabases.stream().map(featureDatabase -> {
                        SqlDelightDatabase database = project.getObjects().newInstance(SqlDelightDatabase.class, featureDatabase.getName());

                        database.getPackageName().set(featureDatabase.getPackageName());
                        database.getVerifyDefinitions().set(featureDatabase.getVerifyDefinitions());
                        database.getVerifyMigrations().set(featureDatabase.getVerifyMigrations());
                        database.getDeriveSchemaFromMigrations().set(featureDatabase.getDeriveSchemaFromMigrations());
                        database.getGenerateAsync().set(featureDatabase.getGenerateAsync());

                        return database;
                    }).collect(Collectors.toUnmodifiableSet()));

                        buildModel.getSqlDelightExtension().getDatabases().addAllLater(sqlDelightDatabases);
                }).withBuildModelImplementationType(DefaultSqlDelightBuildModel.class);
        }
    }
}
