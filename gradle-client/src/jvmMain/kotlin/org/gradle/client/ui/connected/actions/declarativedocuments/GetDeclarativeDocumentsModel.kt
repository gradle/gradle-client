package org.gradle.client.ui.connected.actions.declarativedocuments

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import org.gradle.client.build.model.ResolvedDomPrerequisites
import org.gradle.client.core.gradle.dcl.analyzer
import org.gradle.client.core.gradle.dcl.settingsWithNoOverlayOrigin
import org.gradle.internal.declarativedsl.dom.operations.overlay.DocumentOverlayResult
import org.gradle.internal.declarativedsl.evaluator.main.AnalysisDocumentUtils
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

internal class GetDeclarativeDocumentsModel(private val model: ResolvedDomPrerequisites) {
    private val analyzer = analyzer(model)
    private val _selectedDocument = mutableStateOf<File>(model.declarativeFiles.first())
    private val selectedFileContent = mutableStateOf(readDocumentFile())
    private val settingsFileContent = mutableStateOf(readSettingsFile())
    private val _highlightedSourceRangeByFileId = mutableStateOf(listOf<HighlightingEntry>())
    private fun readDocumentFile() = _selectedDocument.value.takeIf { it.canRead() }?.readText().orEmpty()
    private fun readSettingsFile() = model.settingsFile.takeIf { it.canRead() }?.readText().orEmpty()
    
    val typeRefContext = JointTypeRefContext(
        listOf(model.settingsInterpretationSequence, model.projectInterpretationSequence)
    )

    val selectedDocument: State<File> get() = _selectedDocument

    fun isViewingSettings() = selectedDocument.value == model.settingsFile

    val highlightedSourceRangeByFileId: State<List<HighlightingEntry>> get() = _highlightedSourceRangeByFileId

    fun clearHighlighting() {
        _highlightedSourceRangeByFileId.value = emptyList()
    }

    fun setHighlightingRanges(entries: List<HighlightingEntry>) {
        _highlightedSourceRangeByFileId.value = entries
    }

    fun selectDocument(file: File) {
        _selectedDocument.value = file
        updateFileContents()
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
            clearHighlighting()
        }
    }

    val selectedDocumentResult by derivedStateOf {
        analyzer.evaluate(selectedDocument.value.name, selectedFileContent.value)
    }

    private val settingsResult by derivedStateOf {
        analyzer.evaluate(model.settingsFile.name, settingsFileContent.value)
    }

    val fullModelDom by derivedStateOf {
        if (isViewingSettings())
            settingsWithNoOverlayOrigin(settingsResult)
        else
            AnalysisDocumentUtils.documentWithModelDefaults(settingsResult, selectedDocumentResult)
    }

    fun sourceHighlightingContextFor(overlay: DocumentOverlayResult) =
        HighlightingContext(this, overlay.overlayNodeOriginContainer)

    @Composable
    fun periodicallyUpdateContent() {
        LaunchedEffect(model) {
            while (true) {
                delay(300.milliseconds)
                updateFileContents()
            }
        }
    }
}

internal data class HighlightingEntry(
    val fileIdentifier: String,
    val range: IntRange,
    val highlightingKind: HighlightingKind,
)

internal enum class HighlightingKind {
    EFFECTIVE, SHADOWED;

    fun highlightingColor(): Color = when (this) {
        EFFECTIVE -> Color.Green
        SHADOWED -> Color.Yellow
    }
}
