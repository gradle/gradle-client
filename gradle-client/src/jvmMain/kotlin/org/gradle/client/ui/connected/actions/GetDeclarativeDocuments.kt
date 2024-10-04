package org.gradle.client.ui.connected.actions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.onClick
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.times
import kotlinx.coroutines.delay
import org.gradle.client.build.action.GetResolvedDomAction
import org.gradle.client.build.model.ResolvedDomPrerequisites
import org.gradle.client.core.gradle.dcl.*
import org.gradle.client.ui.build.BuildTextField
import org.gradle.client.ui.composables.*
import org.gradle.client.ui.connected.TwoPanes
import org.gradle.client.ui.theme.spacing
import org.gradle.client.ui.theme.transparency
import org.gradle.declarative.dsl.evaluation.EvaluationSchema
import org.gradle.declarative.dsl.schema.*
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument.DocumentNode.ElementNode
import org.gradle.internal.declarativedsl.dom.DocumentNodeContainer
import org.gradle.internal.declarativedsl.dom.DocumentResolution.ElementResolution.SuccessfulElementResolution.ContainerElementResolved
import org.gradle.internal.declarativedsl.dom.DocumentResolution.PropertyResolution.PropertyAssignmentResolved
import org.gradle.internal.declarativedsl.dom.data.NodeData
import org.gradle.internal.declarativedsl.dom.data.collectToMap
import org.gradle.internal.declarativedsl.dom.mutation.*
import org.gradle.internal.declarativedsl.dom.operations.overlay.DocumentOverlayResult
import org.gradle.internal.declarativedsl.dom.operations.overlay.OverlayNodeOrigin.*
import org.gradle.internal.declarativedsl.dom.operations.overlay.OverlayOriginContainer
import org.gradle.internal.declarativedsl.dom.resolution.DocumentResolutionContainer
import org.gradle.internal.declarativedsl.evaluator.main.AnalysisDocumentUtils.documentWithModelDefaults
import org.gradle.internal.declarativedsl.evaluator.main.AnalysisDocumentUtils.resolvedDocument
import org.gradle.internal.declarativedsl.evaluator.main.AnalysisSequenceResult
import org.gradle.internal.declarativedsl.evaluator.runner.stepResultOrPartialResult
import org.gradle.tooling.BuildAction
import org.jetbrains.skiko.Cursor
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

private const val NOT_DECLARED = "Not declared"
private const val NOTHING_DECLARED = "Nothing declared"

class GetDeclarativeDocuments : GetModelAction.GetCompositeModelAction<ResolvedDomPrerequisites> {

    override val displayName: String
        get() = "Declarative Documents"

    override val modelType = ResolvedDomPrerequisites::class

    override val buildAction: BuildAction<ResolvedDomPrerequisites> = GetResolvedDomAction()

    @Composable
    @Suppress("LongMethod")
    override fun ColumnScope.ModelContent(model: ResolvedDomPrerequisites) {
        val selectedDocument = remember { mutableStateOf<File>(model.declarativeFiles.first()) }

        fun readDocumentFile() = selectedDocument.value.takeIf { it.canRead() }?.readText().orEmpty()

        fun readSettingsFile() = model.settingsFile.takeIf { it.canRead() }?.readText().orEmpty()

        val selectedFileContent = remember(selectedDocument.value) {
            mutableStateOf(readDocumentFile())
        }
        val settingsFileContent = remember(selectedDocument.value, model.settingsFile) {
            mutableStateOf(readSettingsFile())
        }

        fun isViewingSettings() = selectedDocument.value == model.settingsFile

        val highlightedSourceRangeByFileId = remember {
            mutableStateOf(mapOf<String, IntRange>())
        }

        fun updateFileContents() {
            var changed = false
            val newBuildFileContent = readDocumentFile()
            if (newBuildFileContent != selectedFileContent.value) {
                changed = true
                selectedFileContent.value = newBuildFileContent
            }
            val newSettingsFileContent = readSettingsFile()
            if (newSettingsFileContent != settingsFileContent.value) {
                changed = true
                settingsFileContent.value = newSettingsFileContent
            }
            if (changed) {
                highlightedSourceRangeByFileId.value = emptyMap()
            }
        }

        LaunchedEffect(Unit) {
            while (true) {
                delay(300.milliseconds)
                updateFileContents()
            }
        }


        val analyzer = analyzer(model)

        val selectedDocumentResult by derivedStateOf {
            analyzer.evaluate(selectedDocument.value.name, selectedFileContent.value)
        }
        val settingsResult by remember {
            derivedStateOf {
                analyzer.evaluate(model.settingsFile.name, settingsFileContent.value)
            }
        }

        val fullModelDom = if (isViewingSettings())
            settingsWithNoOverlayOrigin(settingsResult)
        else
            documentWithModelDefaults(settingsResult, selectedDocumentResult)

        val hasAnyModelDefaultsContent =
            fullModelDom?.overlayNodeOriginContainer?.collectToMap(fullModelDom.document)
                ?.any { it.value !is FromOverlay } == true

        TitleLarge(displayName)
        DeclarativeFileDropDown(
            model.rootDir,
            model.declarativeFiles,
            selectedDocument
        )
        MaterialTheme.spacing.VerticalLevel4()
        TwoPanes(
            leftWeight = 0.4f, rightWeight = 0.6f,
            verticallyScrollable = false,
            horizontallyScrollable = true,
            left = {
                if (fullModelDom != null) {
                    val softwareTypeNode = fullModelDom.document.singleSoftwareTypeNode
                    if (isViewingSettings()) {
                        SettingsModelContent(highlightedSourceRangeByFileId, selectedDocumentResult)
                    } else {
                        if (softwareTypeNode != null) {
                            ProjectModelContent(
                                selectedDocumentResult,
                                fullModelDom,
                                softwareTypeNode,
                                selectedDocument,
                                highlightedSourceRangeByFileId,
                                ::updateFileContents
                            )
                        } else {
                            Text("No software type")
                        }
                    }
                }
            },
            right = {
                if (fullModelDom != null) {
                    SourcesView(
                        fullModelDom,
                        highlightedSourceRangeByFileId,
                        !isViewingSettings() && hasAnyModelDefaultsContent
                    )
                }
            },
        )
    }

    @Composable
    @Suppress("LongParameterList")
    private fun ProjectModelContent(
        selectedDocumentResult: AnalysisSequenceResult,
        domWithDefaults: DocumentOverlayResult,
        softwareTypeNode: ElementNode,
        selectedDocument: MutableState<File>,
        highlightedSourceRangeByFileId: MutableState<Map<String, IntRange>>,
        updateFileContents: () -> Unit
    ) {
        val highlightingContext = HighlightingContext(
            domWithDefaults.overlayNodeOriginContainer,
            highlightedSourceRangeByFileId
        )

        val projectEvaluationSchemas = selectedDocumentResult.stepResults.values.last()
            .stepResultOrPartialResult.evaluationSchema

        val projectAnalysisSchema = projectEvaluationSchemas.analysisSchema

        val mutationApplicability =
            MutationUtils.checkApplicabilityForOverlay(projectAnalysisSchema, domWithDefaults)

        val softwareTypeSchema = projectAnalysisSchema.softwareTypeNamed(softwareTypeNode.name)
        if (softwareTypeSchema == null) {
            Text("No software type named '${softwareTypeNode.name}'")
        } else {

            val softwareTypeType =
                projectAnalysisSchema.configuredTypeOf(softwareTypeSchema.softwareTypeSemantics)

            ModelView(
                selectedDocument,
                highlightingContext,
                projectEvaluationSchemas,
                domWithDefaults,
                softwareTypeNode,
                softwareTypeType,
                mutationApplicability,
                updateFileContents,
            )
        }
    }

    @Composable
    private fun SettingsModelContent(
        highlightedSourceRangeByFileId: MutableState<Map<String, IntRange>>,
        selectedDocumentResult: AnalysisSequenceResult
    ) {
        selectedDocumentResult.stepResults.forEach { (step, result) ->
            val document = result.stepResultOrPartialResult.resolvedDocument()

            /**
             * For multistep settings, we want to avoid document content appearing as duplicates in
             * multiple steps, so isolate each step's results within a separate document.
             * This separate document does not have any resolution results for the parts resolved in the other steps,
             * so functions visiting the schema+DOM will skip those subtrees.
             */
            val overlayForJustThisDocument = indexBasedOverlayResultFromDocuments(listOf(document))

            val highlightingContext = HighlightingContext(
                overlayForJustThisDocument.overlayNodeOriginContainer,
                highlightedSourceRangeByFileId
            )

            with(
                ModelTreeRendering(
                    indexBasedMultiResolutionContainer(listOf(document)),
                    highlightingContext,
                    NoApplicableMutationsNodeData(),
                    { _, _ -> }
                )
            ) {
                val topLevelReceiverType =
                    result.stepResultOrPartialResult.evaluationSchema.analysisSchema.topLevelReceiverType
                TitleLarge("Step: ${step.stepIdentifier.key}")
                ElementInfoOrNothingDeclared(topLevelReceiverType, document.document, 0)
            }
        }
    }

    @Composable
    @Suppress("LongParameterList")
    private fun ModelView(
        selectedBuildFile: MutableState<File>,
        highlightingContext: HighlightingContext,
        projectEvaluationSchema: EvaluationSchema,
        domWithDefaults: DocumentOverlayResult,
        softwareTypeNode: ElementNode,
        softwareTypeType: DataClass,
        mutationApplicability: NodeData<List<ApplicableMutation>>,
        updateFileContents: () -> Unit
    ) {
        Column {
            with(
                ModelTreeRendering(
                    domWithDefaults.overlayResolutionContainer,
                    highlightingContext,
                    mutationApplicability,
                    onRunMutation = { mutationDefinition, mutationArgumentsContainer ->
                        MutationUtils.runMutation(
                            selectedBuildFile.value,
                            domWithDefaults.inputOverlay,
                            projectEvaluationSchema,
                            mutationDefinition,
                            mutationArgumentsContainer
                        )
                        // Trigger recomposition:
                        updateFileContents()
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
    private fun SourcesView(
        domWithDefaults: DocumentOverlayResult,
        highlightedSourceRangeByFileId: MutableState<Map<String, IntRange>>,
        hasAnyModelDefaultsContent: Boolean
    ) {
        val buildDocument = domWithDefaults.inputOverlay.document
        val settingsDocument = domWithDefaults.inputUnderlay.document

        val (buildFileId, settingsFileId) =
            listOf(buildDocument, settingsDocument).map { it.sourceIdentifier.fileIdentifier }

        val (buildFileContent, settingsFileContent) =
            listOf(buildDocument, settingsDocument).map { it.sourceData.text() }

        val (buildErrorRanges, settingsErrorRanges) =
            listOf(domWithDefaults.result, domWithDefaults.inputUnderlay).map { it.errorRanges() }

        val sources = listOfNotNull(
            SourceFileViewInput(
                buildFileId,
                buildFileContent,
                relevantIndicesRange = null, // we are interested in the whole project file content
                highlightedSourceRange = highlightedSourceRangeByFileId.value[buildFileId],
                errorRanges = buildErrorRanges
            ),
            SourceFileViewInput(
                settingsFileId,
                settingsFileContent,
                // Trim the settings file to just the part that contributed the relevant conventions:
                relevantIndicesRange = domWithDefaults.inputUnderlay.document.relevantRange(),
                highlightedSourceRange = highlightedSourceRangeByFileId.value[settingsFileId],
                errorRanges = settingsErrorRanges
            ).takeIf { hasAnyModelDefaultsContent },
        )

        SourcesColumn(sources) { fileIdentifier, clickOffset ->
            val clickedDocument = when (fileIdentifier) {
                buildFileId -> domWithDefaults.inputOverlay
                settingsFileId -> domWithDefaults.inputUnderlay
                else -> null
            }
            val clickedNode = clickedDocument?.document?.nodeAt(fileIdentifier, clickOffset)
            if (clickedNode == null) {
                highlightedSourceRangeByFileId.value = emptyMap()
            } else {
                highlightedSourceRangeByFileId.value = mapOf(
                    fileIdentifier to clickedNode.sourceData.indexRange
                )
            }
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun DeclarativeFileDropDown(
        rootDir: File,
        declarativeBuildFiles: List<File>,
        state: MutableState<File>
    ) {
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            BuildTextField(
                modifier = Modifier.menuAnchor(),
                value = state.value.relativeTo(rootDir).path,
                onValueChange = { state.value = rootDir.resolve(it) },
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
                            state.value = file
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}

private
class ModelTreeRendering(
    val resolutionContainer: DocumentResolutionContainer,
    val highlightingContext: HighlightingContext,
    val mutationApplicability: NodeData<List<ApplicableMutation>>,
    val onRunMutation: (MutationDefinition, MutationArgumentContainer) -> Unit
) {
    private val indentDp = MaterialTheme.spacing.level2

    @Composable
    fun ModelTreeRendering.ElementInfoOrNothingDeclared(
        type: DataClass?,
        node: DocumentNodeContainer?,
        indentLevel: Int,
    ) {
        if (node is ElementNode && resolutionContainer.isUnresolvedBase(node))
            return

        Column(Modifier.padding(start = indentLevel * indentDp)) {
            if (node == null || type == null) {
                LabelMedium(modifier = Modifier.alpha(MaterialTheme.transparency.HALF), text = NOT_DECLARED)
            } else {
                type.properties.forEach { property ->
                    PropertyInfo(node.property(property.name), property)
                }
                val accessAndConfigure = type.memberFunctions.accessAndConfigure
                val accessAndConfigureNames = accessAndConfigure.map { it.simpleName }
                accessAndConfigure.forEach { subFunction ->
                    ConfiguringFunctionInfo(subFunction, node, indentLevel)
                    MaterialTheme.spacing.VerticalLevel2()
                }
                val addAndConfigure = type.memberFunctions.addAndConfigure.filter { function ->
                    function.simpleName !in accessAndConfigureNames
                }
                val addAndConfigureByName = addAndConfigure.associateBy { it.simpleName }
                val elementsByAddAndConfigure = node.content
                    .filterIsInstance<ElementNode>()
                    .filter { it.name in addAndConfigureByName }

                elementsByAddAndConfigure.forEach { element ->
                    AddingFunctionInfo(element, indentLevel)
                }
            }
        }
    }

    @Composable
    private fun ModelTreeRendering.AddingFunctionInfo(
        element: ElementNode,
        indentLevel: Int
    ) {
        if (resolutionContainer.isUnresolvedBase(element)) return

        val arguments = elementArgumentsString(element)
        val elementType = (resolutionContainer.data(element) as? ContainerElementResolved)?.elementType as? DataClass

        val elementTextRepresentation = "${element.name}($arguments)"

        val isEmpty = elementType == null || element.content.isEmpty()
        WithDecoration(element) {
            if (isEmpty) {
                LabelMedium(
                    modifier = Modifier.padding(bottom = MaterialTheme.spacing.level2)
                        .withHoverCursor()
                        .withClickTextRangeSelection(element, highlightingContext)
                        .semiTransparentIfNull(element),
                    text = elementTextRepresentation
                )
            } else {
                TitleSmall(
                    text = elementTextRepresentation,
                    modifier = Modifier
                        .withHoverCursor()
                        .withClickTextRangeSelection(element, highlightingContext)
                        .semiTransparentIfNull(element)
                )
            }
        }
        if (!isEmpty) {
            ElementInfoOrNothingDeclared(
                elementType,
                element,
                indentLevel + 1
            )
        }
    }

    private fun elementArgumentsString(element: ElementNode) =
        element.elementValues.joinToString { valueNode ->
            when (valueNode) {
                is DeclarativeDocument.ValueNode.LiteralValueNode -> valueNode.value.toString()
                is DeclarativeDocument.ValueNode.NamedReferenceNode -> valueNode.referenceName
                is DeclarativeDocument.ValueNode.ValueFactoryNode -> {
                    val args = valueNode.values.map {
                        (it as? DeclarativeDocument.ValueNode.LiteralValueNode)?.value ?: "..."
                    }
                    val argsString = args.joinToString(",", "(", ")")
                    "${valueNode.factoryName}$argsString"
                }
            }
        }

    @Composable
    private fun ModelTreeRendering.ConfiguringFunctionInfo(
        subFunction: SchemaMemberFunction,
        parentNode: DocumentNodeContainer,
        indentLevel: Int
    ) {
        if (parentNode is ElementNode && resolutionContainer.isUnresolvedBase(parentNode))
            return

        val functionNodes = parentNode.childElementNodes(resolutionContainer, subFunction)
        if (functionNodes.isNotEmpty()) {
            functionNodes.forEach { functionNode ->
                val functionType = functionNode.type(resolutionContainer) as? DataClass

                WithDecoration(functionNode) {
                    val argsString =
                        if (functionNode.elementValues.isNotEmpty()) "(${elementArgumentsString(functionNode)})" else ""
                    val title = subFunction.simpleName + argsString
                    TitleSmall(
                        text = title,
                        modifier = Modifier
                            .withHoverCursor()
                            .semiTransparentIfNull(functionType)
                            .withClickTextRangeSelection(functionNode, highlightingContext)
                    )
                }
                ElementInfoOrNothingDeclared(functionType, functionNode, indentLevel + 1)
            }
        } else {
            TitleSmall(
                text = subFunction.simpleName,
                modifier = Modifier
                    .withHoverCursor()
                    .semiTransparentIfNull(null)
                    .withClickTextRangeSelection(null, highlightingContext)
            )
            ElementInfoOrNothingDeclared(null, null, indentLevel + 1)
        }
    }


    @Composable
    private fun PropertyInfo(
        propertyNode: DeclarativeDocument.DocumentNode.PropertyNode?,
        property: DataProperty
    ) {
        if (propertyNode != null && resolutionContainer.isUnresolvedBase(propertyNode)) {
            return
        }

        WithDecoration(propertyNode) {
            val maybeInvalidDecoration =
                if (propertyNode != null && resolutionContainer.data(propertyNode) !is PropertyAssignmentResolved)
                    TextDecoration.LineThrough else TextDecoration.None
            LabelMedium(
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.level2)
                    .withHoverCursor()
                    .withClickTextRangeSelection(propertyNode, highlightingContext)
                    .semiTransparentIfNull(propertyNode),
                textStyle = TextStyle(textDecoration = maybeInvalidDecoration),
                text = "${property.name}: ${property.typeName} = ${
                    propertyNode?.value?.sourceData?.text() ?: NOTHING_DECLARED
                }"
            )
        }
    }

    @Composable
    fun WithDecoration(
        node: DeclarativeDocument.DocumentNode?,
        content: @Composable () -> Unit
    ) {
        WithApplicableMutations(node) {
            WithModelHighlighting(node) {
                content()
            }
        }
    }

    @Composable
    private fun WithModelHighlighting(
        propertyNode: DeclarativeDocument.DocumentNode?,
        content: @Composable () -> Unit,
    ) {
        val sourceData = propertyNode?.sourceData
        val isHighlighted = sourceData?.indexRange?.let { propRange ->
            highlightingContext.highlightedSourceRange.value[sourceData.sourceIdentifier.fileIdentifier]
                ?.let { highlightedRange ->
                    propRange.first >= highlightedRange.first && propRange.last <= highlightedRange.last
                }
        }
        if (isHighlighted == true) {
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = Color.Yellow,
            ) {
                content()
            }
        } else {
            content()
        }
    }

    @Composable
    fun WithApplicableMutations(
        element: DeclarativeDocument.DocumentNode?,
        content: @Composable () -> Unit
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.level1),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()

            if (element != null) {
                ApplicableMutations(element)
            }
        }
    }

    @Composable
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun ApplicableMutations(node: DeclarativeDocument.DocumentNode) {
        val applicableMutations = mutationApplicability.data(node)
        if (applicableMutations.isNotEmpty()) {
            var isMutationMenuVisible by remember { mutableStateOf(false) }
            val tooltip = "Applicable mutations"
            PlainTextTooltip(tooltip) {
                IconButton(
                    modifier = Modifier
                        .padding(MaterialTheme.spacing.level0)
                        .sizeIn(maxWidth = MaterialTheme.spacing.level6, maxHeight = MaterialTheme.spacing.level6),
                    onClick = { isMutationMenuVisible = true }
                ) {
                    Icon(
                        Icons.Default.Edit,
                        modifier = Modifier.size(MaterialTheme.spacing.level6),
                        contentDescription = tooltip
                    )
                }
            }
            DropdownMenu(
                expanded = isMutationMenuVisible,
                onDismissRequest = { isMutationMenuVisible = false },
            ) {
                var selectedMutation by remember { mutableStateOf<ApplicableMutation?>(null) }
                when (val mutation = selectedMutation) {
                    null -> {
                        MutationDropDownTitle(tooltip)
                        applicableMutations.forEach { applicableMutation ->
                            ListItem(
                                leadingContent = {
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        applicableMutation.mutationDefinition.name
                                    )
                                },
                                headlineContent = { Text(applicableMutation.mutationDefinition.name) },
                                supportingContent = { Text(applicableMutation.mutationDefinition.description) },
                                modifier = Modifier.selectable(selected = false, onClick = {
                                    selectedMutation = applicableMutation
                                }),
                            )
                        }
                    }

                    else -> {
                        var mutationArguments: List<MutationArgumentState> by remember {
                            mutableStateOf(mutation.mutationDefinition.parameters.map { parameter ->
                                when (parameter.kind) {
                                    MutationParameterKind.BooleanParameter ->
                                        MutationArgumentState.BooleanArgument(parameter)

                                    MutationParameterKind.IntParameter ->
                                        MutationArgumentState.IntArgument(parameter)

                                    MutationParameterKind.StringParameter ->
                                        MutationArgumentState.StringArgument(parameter)
                                }
                            })
                        }
                        val validArguments by derivedStateOf {
                            mutationArguments.all { argument ->
                                when (argument) {
                                    is MutationArgumentState.BooleanArgument -> argument.value != null
                                    is MutationArgumentState.IntArgument -> argument.value != null
                                    is MutationArgumentState.StringArgument -> argument.value?.isNotBlank() == true
                                }
                            }
                        }
                        MutationDropDownTitle(
                            headline = mutation.mutationDefinition.name,
                            supporting = mutation.mutationDefinition.description
                        )
                        mutationArguments.forEachIndexed { index, argument ->
                            when (argument) {
                                is MutationArgumentState.BooleanArgument ->
                                    ListItem(
                                        headlineContent = { Text(argument.parameter.name) },
                                        supportingContent = { Text(argument.parameter.description) },
                                        trailingContent = {
                                            Checkbox(
                                                checked = argument.value ?: false,
                                                onCheckedChange = { newChecked ->
                                                    mutationArguments = mutationArguments.toMutableList().apply {
                                                        this[index] = argument.copy(value = newChecked)
                                                    }
                                                }
                                            )
                                        }
                                    )

                                is MutationArgumentState.IntArgument ->
                                    ListItem(headlineContent = {
                                        OutlinedTextField(
                                            modifier = Modifier.fillMaxWidth(),
                                            label = { Text(argument.parameter.name) },
                                            placeholder = { Text(argument.parameter.description) },
                                            value = argument.value?.toString() ?: "",
                                            onValueChange = { newValue ->
                                                mutationArguments = mutationArguments.toMutableList().apply {
                                                    this[index] = argument.copy(value = newValue.toIntOrNull())
                                                }
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        )
                                    })

                                is MutationArgumentState.StringArgument ->
                                    ListItem(headlineContent = {
                                        OutlinedTextField(
                                            modifier = Modifier.fillMaxWidth(),
                                            label = { Text(argument.parameter.name) },
                                            placeholder = { Text(argument.parameter.description) },
                                            value = argument.value ?: "",
                                            onValueChange = { newValue ->
                                                mutationArguments = mutationArguments.toMutableList().apply {
                                                    this[index] = argument.copy(value = newValue)
                                                }
                                            }
                                        )
                                    })
                            }
                        }
                        ListItem(headlineContent = {}, trailingContent = {
                            Button(
                                content = {
                                    val text = "Apply mutation"
                                    Icon(Icons.Default.Edit, text)
                                    MaterialTheme.spacing.HorizontalLevel2()
                                    Text(text)
                                },
                                enabled = validArguments,
                                onClick = {
                                    onRunMutation(
                                        mutation.mutationDefinition,
                                        mutationArguments.toMutationArgumentsContainer()
                                    )
                                    isMutationMenuVisible = false
                                },
                            )
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun MutationDropDownTitle(
    headline: String,
    supporting: String? = null
) {
    ListItem(
        headlineContent = { TitleMedium(headline) },
        supportingContent = supporting?.let { { Text(supporting) } },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            headlineColor = MaterialTheme.colorScheme.onSecondaryContainer,
            supportingColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    )

}

private sealed interface MutationArgumentState {

    val parameter: MutationParameter<*>

    data class IntArgument(
        override val parameter: MutationParameter<*>,
        val value: Int? = null,
    ) : MutationArgumentState

    data class StringArgument(
        override val parameter: MutationParameter<*>,
        val value: String? = null
    ) : MutationArgumentState

    data class BooleanArgument(
        override val parameter: MutationParameter<*>,
        val value: Boolean? = null
    ) : MutationArgumentState
}

@Suppress("UNCHECKED_CAST")
private fun List<MutationArgumentState>.toMutationArgumentsContainer(): MutationArgumentContainer =
    mutationArguments {
        forEach { argumentState ->
            when (argumentState) {
                is MutationArgumentState.IntArgument ->
                    argument(argumentState.parameter as MutationParameter<Int>, requireNotNull(argumentState.value))

                is MutationArgumentState.StringArgument ->
                    argument(
                        argumentState.parameter as MutationParameter<String>,
                        requireNotNull(argumentState.value)
                    )

                is MutationArgumentState.BooleanArgument ->
                    argument(
                        argumentState.parameter as MutationParameter<Boolean>,
                        requireNotNull(argumentState.value)
                    )
            }
        }
    }


private data class HighlightingContext(
    val overlayOriginContainer: OverlayOriginContainer,
    val highlightedSourceRange: MutableState<Map<String, IntRange>>
)

private fun Modifier.withHoverCursor() =
    pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.withClickTextRangeSelection(
    node: DeclarativeDocument.DocumentNode?,
    highlightingContext: HighlightingContext
) = onClick {
    fun DeclarativeDocument.DocumentNode.sourceIdentifierToRange() =
        sourceData.sourceIdentifier.fileIdentifier to sourceData.indexRange

    highlightingContext.highlightedSourceRange.value = if (node == null) {
        emptyMap()
    } else {
        val ownIdToRange = node.sourceIdentifierToRange()
        when (val origin = highlightingContext.overlayOriginContainer.data(node)) {
            is FromOverlay,
            is FromUnderlay -> mapOf(ownIdToRange)

            is MergedElements -> mapOf(ownIdToRange, origin.underlayElement.sourceIdentifierToRange())
            is ShadowedProperty -> mapOf(ownIdToRange, origin.underlayProperty.sourceIdentifierToRange())
        }
    }
}
