package org.gradle.client.ui.connected.actions

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import org.gradle.client.ui.composables.LabelSmall
import org.gradle.client.ui.composables.TitleMedium
import org.gradle.client.ui.composables.horizontalScrollContent
import org.gradle.tooling.model.build.BuildEnvironment

class GetBuildEnvironment : GetModelAction<BuildEnvironment> {

    override val modelType = BuildEnvironment::class

    override val displayName: String
        get() = "Build Environment"

    @Composable
    override fun ColumnScope.ModelContent(model: BuildEnvironment) {
        horizontalScrollContent {
            TitleMedium("Build Environment")
            LabelSmall("Build Identifier: ${model.buildIdentifier.rootDir}")
            LabelSmall("Java Home: ${model.java.javaHome}")
            LabelSmall("JVM Arguments:  ${model.java.jvmArguments}")
            LabelSmall("Gradle Version: ${model.gradle.gradleVersion}")
            LabelSmall("Gradle User Home: ${model.gradle.gradleUserHome}")
        }
    }
}
