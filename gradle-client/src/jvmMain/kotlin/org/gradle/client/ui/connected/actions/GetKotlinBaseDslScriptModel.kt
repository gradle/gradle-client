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
import org.gradle.tooling.BuildAction
import org.gradle.tooling.BuildController
import org.gradle.tooling.model.dsl.KotlinDslBaseScriptModel

class GetKotlinBaseDslScriptModel : GetModelAction.GetCompositeModelAction<KotlinDslBaseScriptModel> {

    override val modelType = KotlinDslBaseScriptModel::class

    override val buildAction: BuildAction<KotlinDslBaseScriptModel> = KotlinDslBaseScriptModelAction()

    override val displayName: String
        get() = "Kotlin DSL Base Model"

    @Composable
    override fun ColumnScope.ModelContent(model: KotlinDslBaseScriptModel) {
        horizontalScrollContent {
            Text(
                text = "Kotlin DSL Base Model",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.size(MaterialTheme.spacing.level2))
            Text(
                text = "Script Templates ClassPath, number of entries: ${model.scriptTemplatesClassPath.size}",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = "Kotlin DSL Base ClassPath, number of entries: ${model.compileClassPath.size}",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = "Implicit imports, number of imports: ${model.implicitImports.size}",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }

    class KotlinDslBaseScriptModelAction: BuildAction<KotlinDslBaseScriptModel> {
        override fun execute(controller: BuildController): KotlinDslBaseScriptModel {
            return controller.getModel(KotlinDslBaseScriptModel::class.java)
        }
    }
}
