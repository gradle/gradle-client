package org.gradle.client.ui.connected.actions

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import org.gradle.client.ui.composables.LabelSmall
import org.gradle.client.ui.composables.TitleMedium
import org.gradle.client.ui.composables.horizontalScrollContent
import org.gradle.tooling.model.GradleProject

class GetGradleProject : GetModelAction<GradleProject> {

    override val modelType = GradleProject::class

    override val displayName: String
        get() = "Projects Model"

    @Composable
    override fun ColumnScope.ModelContent(model: GradleProject) {
        horizontalScrollContent {
            TitleMedium("Gradle Project")
            LabelSmall("Root Project Identifier: ${model.projectIdentifier}")
            LabelSmall("Root Project Name: ${model.name}")
            LabelSmall("Root Project Description: ${model.description}")
        }
    }
}
