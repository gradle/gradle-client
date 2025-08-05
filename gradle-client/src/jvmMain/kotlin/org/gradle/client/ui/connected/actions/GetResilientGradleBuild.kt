package org.gradle.client.ui.connected.actions

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import org.gradle.tooling.BuildAction
import org.gradle.tooling.BuildController
import org.gradle.tooling.model.gradle.GradleBuild
import java.io.Serializable

class GetResilientGradleBuild : GetModelAction.GetCompositeModelAction<GetResilientGradleBuild.ResilientGradleBuild> {

    override val buildAction: BuildAction<ResilientGradleBuild> = ResilientGradleBuildAction()

    override val modelType = ResilientGradleBuild::class

    override val displayName: String
        get() = "Resilient Build Model"

    @Composable
    override fun ColumnScope.ModelContent(model: ResilientGradleBuild) {
        GetGradleBuild().run {
            ModelContent(model.gradleBuild)
        }
    }


    class ResilientGradleBuildAction: BuildAction<ResilientGradleBuild> {
        override fun execute(controller: BuildController): ResilientGradleBuild {
            // TODO: Query a model via resilient tooling API
            return ResilientGradleBuild(controller.getModel(GradleBuild::class.java))
        }
    }

    data class ResilientGradleBuild(val gradleBuild: GradleBuild): Serializable
}