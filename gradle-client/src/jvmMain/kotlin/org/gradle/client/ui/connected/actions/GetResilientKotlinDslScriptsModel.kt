package org.gradle.client.ui.connected.actions

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.gradle.client.ui.composables.horizontalScrollContent
import org.gradle.client.ui.connected.actions.resilientsync.ResilientModelView
import org.gradle.client.ui.connected.actions.resilientsync.Result
import org.gradle.client.ui.theme.spacing
import org.gradle.tooling.BuildAction
import org.gradle.tooling.BuildController
import org.gradle.tooling.model.kotlin.dsl.KotlinDslScriptModel
import org.gradle.tooling.model.kotlin.dsl.KotlinDslScriptsModel
import java.io.File

class GetResilientKotlinDslScriptsModel : GetModelAction.GetCompositeModelAction<KotlinDslScriptsModel> {

    override val buildAction: BuildAction<KotlinDslScriptsModel> = ResilientKotlinDslScriptsModelAction()

    override val modelType = KotlinDslScriptsModel::class

    override val displayName: String
        get() = "Resilient Kotlin DSL Model"

    @Composable
    override fun ColumnScope.ModelContent(model: KotlinDslScriptsModel) {
        val result = Result(isSuccess = true, list = emptyList())
        ResilientModelView(result) {
            KotlinDslScriptsModel(model)
        }
    }

    @Composable
    private
    fun KotlinDslScriptsModel(model: KotlinDslScriptsModel) {
        horizontalScrollContent {
            Text(
                text = "Kotlin DSL Model",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.size(MaterialTheme.spacing.level2))

            Text(
                text = "Number of Scripts: ${model.scriptModels.size}",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.size(MaterialTheme.spacing.level2))
            model.scriptModels.forEach {
                KotlinDslScriptModel(it.key, it.value)
                Spacer(Modifier.size(MaterialTheme.spacing.level4))
            }
        }
    }

    @Composable
    private
    fun KotlinDslScriptModel(scriptFile: File, model: KotlinDslScriptModel) {
        Text(
            text = "Script: $scriptFile",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "ClassPath, number of entries: ${model.classPath.size}",
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = "SourcePath, number of entries: ${model.sourcePath.size}",
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = "Implicit imports, number of imports: ${model.implicitImports.size}",
            style = MaterialTheme.typography.labelMedium
        )
    }

    class ResilientKotlinDslScriptsModelAction: BuildAction<KotlinDslScriptsModel> {
        override fun execute(controller: BuildController): KotlinDslScriptsModel {
            return controller.getModel(KotlinDslScriptsModel::class.java)
        }
    }
}
