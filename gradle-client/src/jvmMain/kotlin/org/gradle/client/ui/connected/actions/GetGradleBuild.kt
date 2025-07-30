package org.gradle.client.ui.connected.actions

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.gradle.client.ui.composables.horizontalScrollContent
import org.gradle.client.ui.theme.spacing
import org.gradle.tooling.model.gradle.GradleBuild

class GetGradleBuild : GetModelAction<GradleBuild> {

    override val modelType = GradleBuild::class

    override val displayName: String
        get() = "Build Model"

    @Composable
    override fun ColumnScope.ModelContent(model: GradleBuild) {
        horizontalScrollContent {
            Text(
                text = "Root Gradle Build",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Root Project Name: ${model.rootProject.name}",
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "Build Directory: ${model.buildIdentifier.rootDir}",
                style = MaterialTheme.typography.labelSmall
            )

            Spacer(Modifier.size(MaterialTheme.spacing.level4))
            IncludedBuilds(model)

            Spacer(Modifier.size(MaterialTheme.spacing.level4))
            Projects(model)
        }
    }

    @Composable
    private
    fun IncludedBuilds(model: GradleBuild) {
        Text(
            text = "Included Builds (${model.includedBuilds.size}  total)",
            style = MaterialTheme.typography.titleMedium
        )
        model.includedBuilds.forEach { includedBuild ->
            Text(
                text = "${includedBuild.rootProject.name}",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Build Directory: ${includedBuild.buildIdentifier.rootDir}",
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "Build Path: ${includedBuild.rootProject.projectIdentifier.projectPath}",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }

    @Composable
    private
    fun Projects(model: GradleBuild) {
        Text(
            text = "Projects (${model.projects.size}  total)",
            style = MaterialTheme.typography.titleMedium
        )
        model.projects.forEach { project ->
            Text(
                text = "${project.name}",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Project Directory: ${project.projectDirectory}",
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "Project Path: ${project.projectIdentifier.projectPath}",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
