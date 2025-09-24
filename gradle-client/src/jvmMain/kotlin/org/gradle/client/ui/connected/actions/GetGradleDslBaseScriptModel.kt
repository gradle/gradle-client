package org.gradle.client.ui.connected.actions

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import org.gradle.client.ui.composables.LabelMedium
import org.gradle.client.ui.composables.TitleLarge
import org.gradle.client.ui.composables.TitleMedium
import org.gradle.client.ui.composables.horizontalScrollContent
import org.gradle.client.ui.theme.Spacing
import org.gradle.tooling.BuildAction
import org.gradle.tooling.BuildController
import org.gradle.tooling.model.dsl.GradleDslBaseScriptModel

class GetGradleDslBaseScriptModel : GetModelAction.GetCompositeModelAction<GradleDslBaseScriptModel> {

    override val modelType = GradleDslBaseScriptModel::class

    override val buildAction: BuildAction<GradleDslBaseScriptModel> = GradleDslBaseScriptModelAction()

    override val displayName: String
        get() = "Gradle DSL Base Model"

    @Composable
    override fun ColumnScope.ModelContent(model: GradleDslBaseScriptModel) {
        horizontalScrollContent {
            TitleLarge(displayName)
            Spacing.VerticalLevel4()

            TitleMedium("Kotlin DSL")
            LabelMedium("Templates ClassPath size: ${model.kotlinDslBaseScriptModel.scriptTemplatesClassPath.size}")
            LabelMedium("Script Base ClassPath size: ${model.kotlinDslBaseScriptModel.compileClassPath.size}")
            LabelMedium("Implicit imports count: ${model.kotlinDslBaseScriptModel.implicitImports.size}")
            Spacing.VerticalLevel2()

            TitleMedium("Groovy DSL")
            LabelMedium("Script Base ClassPath size: ${model.groovyDslBaseScriptModel.compileClassPath.size}")
            LabelMedium("Implicit imports count: ${model.groovyDslBaseScriptModel.implicitImports.size}")
        }
    }

    class GradleDslBaseScriptModelAction : BuildAction<GradleDslBaseScriptModel> {
        override fun execute(controller: BuildController): GradleDslBaseScriptModel {
            return controller.getModel(GradleDslBaseScriptModel::class.java)
        }
    }
}
