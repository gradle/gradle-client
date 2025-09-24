package org.gradle.client.ui.connected.actions

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import org.gradle.client.ui.composables.LabelSmall
import org.gradle.client.ui.composables.TitleMedium
import org.gradle.client.ui.composables.TitleSmall
import org.gradle.client.ui.composables.horizontalScrollContent
import org.gradle.client.ui.theme.Spacing
import org.gradle.tooling.model.gradle.GradleBuild

class GetGradleBuild : GetModelAction<GradleBuild> {

    override val modelType = GradleBuild::class

    override val displayName: String
        get() = "Build Model"

    @Composable
    override fun ColumnScope.ModelContent(model: GradleBuild) {
        horizontalScrollContent {
            TitleMedium("Root Gradle Build")
            LabelSmall("Root Project Name: ${model.rootProject.name}")
            LabelSmall("Build Directory: ${model.buildIdentifier.rootDir}")

            Spacing.VerticalLevel4()
            IncludedBuilds(model)

            Spacing.VerticalLevel4()
            Projects(model)
        }
    }

    @Composable
    private
    fun IncludedBuilds(model: GradleBuild) {
        TitleMedium("Included Builds (${model.includedBuilds.size}  total)")
        model.includedBuilds.forEach { includedBuild ->
            TitleSmall(includedBuild.rootProject.name)
            LabelSmall("Build Directory: ${includedBuild.buildIdentifier.rootDir}")
            LabelSmall("Build Path: ${includedBuild.rootProject.projectIdentifier.projectPath}")
        }
    }

    @Composable
    private
    fun Projects(model: GradleBuild) {
        TitleMedium("Projects (${model.projects.size}  total)")
        model.projects.forEach { project ->
            TitleSmall(project.name)
            LabelSmall("Project Directory: ${project.projectDirectory}")
            LabelSmall("Project Path: ${project.projectIdentifier.projectPath}")
        }
    }
}
