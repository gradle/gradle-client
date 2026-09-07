package org.gradle.client.build.action;

import org.gradle.client.build.model.ResolvedDomPrerequisites;
import org.gradle.declarative.dsl.evaluation.InterpretationSequence;
import org.gradle.declarative.dsl.schema.AnalysisSchema;
import org.gradle.declarative.dsl.tooling.models.DeclarativeSchemaModel;
import org.gradle.internal.Pair;
import org.gradle.tooling.BuildAction;
import org.gradle.tooling.BuildController;
import org.gradle.tooling.model.gradle.GradleBuild;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class GetResolvedDomAction implements BuildAction<ResolvedDomPrerequisites> {

    @Override
    public ResolvedDomPrerequisites execute(BuildController controller) {
        DeclarativeSchemaModel declarativeSchemaModel = controller.getModel(DeclarativeSchemaModel.class);
        InterpretationSequence settingsSchema = declarativeSchemaModel.getSettingsSequence();
        InterpretationSequence projectSchema = declarativeSchemaModel.getProjectSequence();
        Pair<File, List<ResolvedDomPrerequisites.ProjectInfo>> buildFiles = getProjects(controller);
        return new ResolvedDomPrerequisitesImpl(settingsSchema, projectSchema, buildFiles.getLeft(), buildFiles.getRight());
    }

    private static Pair<File, List<ResolvedDomPrerequisites.ProjectInfo>> getProjects(BuildController controller) {
        GradleBuild gradleBuild = controller.getModel(GradleBuild.class);
        File rootProjectDirectory = gradleBuild.getRootProject().getProjectDirectory();
        List<ResolvedDomPrerequisites.ProjectInfo> declarativeBuildFiles = gradleBuild
            .getProjects()
            .getAll()
            .stream()
            .map(p -> new ProjectInfoImpl(p.getPath(), getBuildFileInProjectDirectory(p.getProjectDirectory())))
            .collect(Collectors.toList());
        if (declarativeBuildFiles.isEmpty()) {
            throw new RuntimeException("No declarative project file found");
        }
        return Pair.of(rootProjectDirectory, declarativeBuildFiles);
    }

    private static final class ProjectInfoImpl implements ResolvedDomPrerequisites.ProjectInfo, Serializable {
        private final String projectPath;
        private final File projectBuildFile;

        @Override
        public String getProjectPath() {
            return projectPath;
        }

        @Override
        public File getProjectBuildFile() {
            return projectBuildFile;
        }

        @Override
        public File getProjectDirectory() {
            return projectBuildFile.getParentFile();
        }

        @Override
        public boolean isDeclarative() {
            return projectBuildFile.getName().endsWith(".dcl") && projectBuildFile.canRead();
        }

        public ProjectInfoImpl(String projectPath, File projectBuildFile) {
            this.projectPath = projectPath;
            this.projectBuildFile = projectBuildFile;
        }
    }

    private static File getBuildFileInProjectDirectory(File projectDir) {
        File dcl = new File(projectDir, "build.gradle.dcl");
        if (dcl.exists())
            return dcl;
        File kts = new File(projectDir, "build.gradle.kts");
        if (kts.exists())
            return kts;
        File groovy = new File(projectDir, "build.gradle");
        if (groovy.exists())
            return groovy;

        return dcl;
    }

    private static final class ResolvedDomPrerequisitesImpl implements ResolvedDomPrerequisites {

        private final InterpretationSequence settingsSequence;
        private final InterpretationSequence projectSequence;
        private final File rootDir;
        private final List<ProjectInfo> projectInfos;

        public ResolvedDomPrerequisitesImpl(
                InterpretationSequence settingsSequence,
                InterpretationSequence projectSequence,
                File rootDir,
                List<ProjectInfo> projectInfos
        ) {
            this.settingsSequence = settingsSequence;
            this.projectSequence = projectSequence;
            this.rootDir = rootDir;
            this.projectInfos = projectInfos;
        }

        @Override
        public InterpretationSequence getSettingsInterpretationSequence() {
            return settingsSequence;
        }

        @Override
        public InterpretationSequence getProjectInterpretationSequence() {
            return projectSequence;
        }

        @Override
        public AnalysisSchema getAnalysisSchema() {
            return StreamSupport.stream(getProjectInterpretationSequence().getSteps().spliterator(), false)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("no schema step available for project"))
                    .getEvaluationSchemaForStep()
                    .getAnalysisSchema();
        }

        @Override
        public File getRootDir() {
            return rootDir;
        }

        @Override
        public File getSettingsFile() {
            // TODO: this is an assumption about the location of the settings file – get it from Gradle instead.
            return new File(getRootDir(), "settings.gradle.dcl");
        }

        @Override
        public List<File> getDeclarativeFiles() {
            return Stream.concat(
                    projectInfos.stream()
                        .filter(f -> f.isDeclarative() && f.getProjectBuildFile().exists())
                        .map(ProjectInfo::getProjectBuildFile),
                    getSettingsFile().canRead() ? Stream.of(getSettingsFile()) : Stream.empty()
            ).collect(Collectors.toList());
        }

        @Override
        public List<ProjectInfo> getProjectInfos() {
            return projectInfos;
        }
    }
}
