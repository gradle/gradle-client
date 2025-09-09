package org.gradle.client.ui.connected.actions.declarativedocuments

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.gradle.client.build.action.GetResolvedDomAction
import org.gradle.client.build.model.ResolvedDomPrerequisites
import org.gradle.client.core.gradle.dcl.*
import org.gradle.client.ui.build.BuildTextField
import org.gradle.client.ui.composables.SourceFileViewInput
import org.gradle.client.ui.composables.SourcesColumn
import org.gradle.client.ui.composables.TitleLarge
import org.gradle.client.ui.composables.TitleMedium
import org.gradle.client.ui.connected.TwoPanes
import org.gradle.client.ui.connected.actions.*
import org.gradle.client.ui.connected.actions.declarativedocuments.HighlightingKind.EFFECTIVE
import org.gradle.client.ui.connected.actions.declarativedocuments.HighlightingTarget.DOCUMENT_NODE
import org.gradle.client.ui.theme.spacing
import org.gradle.internal.declarativedsl.analysis.TypeRefContext
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument
import org.gradle.internal.declarativedsl.dom.data.collectToMap
import org.gradle.internal.declarativedsl.dom.operations.overlay.OverlayNodeOrigin.*
import org.gradle.internal.declarativedsl.dom.operations.overlay.OverlayOriginContainer
import org.gradle.internal.declarativedsl.dom.resolution.DocumentResolutionContainer
import org.gradle.internal.declarativedsl.evaluator.main.AnalysisDocumentUtils.resolvedDocument
import org.gradle.internal.declarativedsl.evaluator.runner.stepResultOrPartialResult
import org.gradle.tooling.BuildAction
import org.jetbrains.skiko.Cursor
import java.io.File
import kotlin.time.Duration.Companion.seconds


class GetDeclarativeDocuments : GetModelAction.GetCompositeModelAction<ResolvedDomPrerequisites> {
    override val displayName: String
        get() = "Declarative Documents"

    override val modelType = ResolvedDomPrerequisites::class

    override val buildAction: BuildAction<ResolvedDomPrerequisites> = GetResolvedDomAction()

    @Composable
    override fun ColumnScope.ModelContent(model: ResolvedDomPrerequisites) {
        val viewModel = rememberSaveable { GetDeclarativeDocumentsModel(model) }
        viewModel.periodicallyUpdateContent()

        TitleLarge(displayName)
        DeclarativeFileDropDown(
            model.rootDir,
            model.declarativeFiles,
            viewModel.selectedDocument.value,
            viewModel::selectDocument
        )
        MaterialTheme.spacing.VerticalLevel4()
        TwoPanes(
            leftWeight = 0.4f, rightWeight = 0.6f,
            verticallyScrollable = false,
            horizontallyScrollable = true,
            left = {
                if (viewModel.isViewingSettings()) {
                    SettingsModelContent(viewModel)
                } else {
                    ProjectModelContent(viewModel)
                }
            },
            right = { SourcesView(viewModel) },
        )
    }

    @Composable
    private fun SettingsModelContent(viewModel: GetDeclarativeDocumentsModel) {
        viewModel.selectedDocumentResult.stepResults.forEach { (step, result) ->
            val document = result.stepResultOrPartialResult.resolvedDocument()

            /**
             * For multistep settings, we want to avoid document content appearing as duplicates in
             * multiple steps, so isolate each step's results within a separate document.
             * This separate document does not have any resolution results for the parts resolved in the other steps,
             * so functions visiting the schema+DOM will skip those subtrees.
             */
            val overlayForJustThisDocument = indexBasedOverlayResultFromDocuments(listOf(document))

            val highlightingContext = viewModel.sourceHighlightingContextFor(overlayForJustThisDocument)

            with(
                ModelTreeRendering(
                    viewModel.typeRefContext,
                    indexBasedMultiResolutionContainer(listOf(document)),
                    overlayForJustThisDocument.overlayNodeOriginContainer,
                    highlightingContext,
                    NoApplicableMutationsNodeData()
                ) { _, _ -> }
            ) {
                TitleLarge("Step: ${step.stepIdentifier.key}")
                val topLevelReceiverType =
                    result.stepResultOrPartialResult.evaluationSchema.analysisSchema.topLevelReceiverType
                ElementInfoOrNothingDeclared(topLevelReceiverType, document.document, 0)
            }
        }
    }

    @Composable
    @Suppress("LongParameterList", "ReturnCount")
    private fun ProjectModelContent(viewModel: GetDeclarativeDocumentsModel) {
        val domWithDefaults = viewModel.fullModelDom ?: return

        val highlightingContext = viewModel.sourceHighlightingContextFor(domWithDefaults)

        val projectEvaluationSchema = viewModel.selectedDocumentResult.stepResults.values.lastOrNull()
            ?.stepResultOrPartialResult?.evaluationSchema
            ?: run {
                Text("Could not get the schema for the document")
                return
            }

        val projectAnalysisSchema = projectEvaluationSchema.analysisSchema

        val softwareTypeNode = viewModel.fullModelDom?.document?.singleSoftwareTypeNode
            ?: run {
                Text("No software type")
                return
            }

        val softwareTypeSchema = projectAnalysisSchema.softwareTypeNamed(softwareTypeNode.name)
            ?: run {
                Text("No software type named '${softwareTypeNode.name}'")
                return
            }

        val softwareTypeType = projectAnalysisSchema.configuredTypeOf(softwareTypeSchema.softwareTypeSemantics)

        val mutationApplicability = run {
            MutationUtils.checkApplicabilityForOverlay(projectAnalysisSchema, domWithDefaults)
        }

        Column {
            with(
                ModelTreeRendering(
                    viewModel.typeRefContext,
                    domWithDefaults.overlayResolutionContainer,
                    domWithDefaults.overlayNodeOriginContainer,
                    highlightingContext,
                    mutationApplicability,
                    onRunMutation = { mutationDefinition, mutationArgumentsContainer ->
                        MutationUtils.runMutation(
                            viewModel.selectedDocument.value,
                            domWithDefaults.inputOverlay,
                            projectEvaluationSchema,
                            mutationDefinition,
                            mutationArgumentsContainer
                        )
                        // Trigger recomposition:
                        viewModel.updateFileContents()
                    }
                )
            ) {
                WithDecoration(softwareTypeNode) {
                    TitleMedium(
                        text = "Software Type: ${softwareTypeNode.name}",
                        modifier = Modifier
                            .pointerHoverIcon(
                                PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
                            )
                            .withClickTextRangeSelection(softwareTypeNode, highlightingContext)
                    )
                }
                MaterialTheme.spacing.VerticalLevel4()

                ElementInfoOrNothingDeclared(softwareTypeType, softwareTypeNode, 0)
            }
        }
    }

    @Composable
    private fun SourcesView(viewModel: GetDeclarativeDocumentsModel) {
        val domWithDefaults = viewModel.fullModelDom ?: return

        val buildDocument = domWithDefaults.inputOverlay.document
        val settingsDocument = domWithDefaults.inputUnderlay.document

        val (buildFileId, settingsFileId) =
            listOf(buildDocument, settingsDocument).map { it.sourceIdentifier.fileIdentifier }

        val (buildFileContent, settingsFileContent) =
            listOf(buildDocument, settingsDocument).map { it.sourceData.text() }

        val (buildErrorRanges, settingsErrorRanges) =
            listOf(domWithDefaults.inputOverlay, domWithDefaults.inputUnderlay).map { it.errorRanges() }

        val hasAnyModelDefaultsContent =
            domWithDefaults.overlayNodeOriginContainer.collectToMap(domWithDefaults.document)
                .any { it.value !is FromOverlay }
        
        val reportedErrors = viewModel.reportedErrors.value

        val sources = listOfNotNull(
            SourceFileViewInput(
                buildFileId,
                buildFileContent,
                relevantIndicesRange = null, // we are interested in the whole project file content
                highlightedSourceRange =
                    viewModel.highlightedSourceRangeByFileId.value.filter { it.fileIdentifier == buildFileId },
                allErrorRanges = buildErrorRanges,
                selectedErrorRanges = reportedErrors[buildFileId].orEmpty()
            ),
            SourceFileViewInput(
                settingsFileId,
                settingsFileContent,
                // Trim the settings file to just the part that contributed the relevant conventions:
                relevantIndicesRange = domWithDefaults.inputUnderlay.document.relevantRange(),
                highlightedSourceRange =
                    viewModel.highlightedSourceRangeByFileId.value.filter { it.fileIdentifier == settingsFileId },
                allErrorRanges = settingsErrorRanges,
                selectedErrorRanges = reportedErrors[settingsFileId].orEmpty()
            ).takeIf { !viewModel.isViewingSettings() && hasAnyModelDefaultsContent },
        )

        SourcesColumn(sources) { fileIdentifier, clickOffset ->
            val clickedDocument = when (fileIdentifier) {
                buildFileId -> domWithDefaults.inputOverlay
                settingsFileId -> domWithDefaults.inputUnderlay
                else -> null
            }
            val clickedNode = clickedDocument?.document?.nodeAt(fileIdentifier, clickOffset)
            if (clickedNode == null) {
                viewModel.clearHighlighting()
            } else {
                viewModel.setHighlightingRanges(
                    findRelevantNodesAndProduceHighlighting(
                        clickedNode,
                        domWithDefaults.document, domWithDefaults.overlayResolutionContainer, viewModel.typeRefContext,
                        domWithDefaults.overlayNodeOriginContainer
                    ) ?: listOf(clickedNode.highlightAs(EFFECTIVE, DOCUMENT_NODE)),
                )
            }
            val errorsByFileId = mapOf(
                fileIdentifier to sources.single { it.fileIdentifier == fileIdentifier }
                    .allErrorRanges.filter { clickOffset in it.range }
            )
            viewModel.reportErrors(errorsByFileId)
            CoroutineScope(Dispatchers.Main).launch { 
                delay(4.seconds)
                if (viewModel.reportedErrors.value === errorsByFileId) {
                    viewModel.reportErrors(emptyMap())
                }
            }
        }
    }

    private fun findRelevantNodesAndProduceHighlighting(
        forNode: DeclarativeDocument.DocumentNode,
        inDocument: DeclarativeDocument,
        resolutionContainer: DocumentResolutionContainer,
        typeRefContext: TypeRefContext,
        overlayOriginContainer: OverlayOriginContainer
    ): List<HighlightingEntry>? =
        overlayOriginContainer.collectToMap(inDocument).entries.firstNotNullOfOrNull { (node, origin) ->
            when (origin) {
                is FromOverlay -> if (origin.documentNode == forNode)
                    highlightingForAllDocumentNodesByModelNode(
                        origin.documentNode,
                        resolutionContainer,
                        typeRefContext,
                        overlayOriginContainer
                    )
                else null

                is FromUnderlay -> if (origin.documentNode == forNode)
                    highlightingForAllDocumentNodesByModelNode(
                        origin.documentNode,
                        resolutionContainer,
                        typeRefContext,
                        overlayOriginContainer
                    )
                else null

                is MergedElements -> if (node is DeclarativeDocument.DocumentNode && (
                        origin.overlayElement == forNode || origin.underlayElement == forNode)
                ) highlightingForAllDocumentNodesByModelNode(
                    node,
                    resolutionContainer,
                    typeRefContext,
                    overlayOriginContainer
                ) else null

                is MergedProperties -> origin.run {
                    if (listOf(
                            shadowedPropertiesFromUnderlay,
                            shadowedPropertiesFromOverlay,
                            effectivePropertiesFromUnderlay,
                            effectivePropertiesFromOverlay
                        ).any { forNode in it }
                    ) (effectivePropertiesFromUnderlay + effectivePropertiesFromOverlay).lastOrNull()
                        ?.let {
                            highlightingForAllDocumentNodesByModelNode(
                                it,
                                resolutionContainer,
                                typeRefContext,
                                overlayOriginContainer
                            )
                        }
                    else null
                }
            }
        }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun DeclarativeFileDropDown(
        rootDir: File,
        declarativeBuildFiles: List<File>,
        currentSelection: File,
        selectDocument: (File) -> Unit
    ) {
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            BuildTextField(
                modifier = Modifier.menuAnchor(),
                value = currentSelection.relativeTo(rootDir).path,
                onValueChange = { selectDocument(rootDir.resolve(it)) },
                readOnly = true,
                label = { Text("Declarative documents") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                declarativeBuildFiles.forEach { file ->
                    DropdownMenuItem(
                        text = { Text(file.relativeTo(rootDir).path) },
                        onClick = {
                            selectDocument(file)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}
