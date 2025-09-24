package org.gradle.client.ui.connected.actions.resilientsync

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.gradle.client.ui.theme.Spacing
import org.gradle.client.ui.theme.spacing

@Composable
fun ColumnScope.ResilientModelView(result: Result, modelContent: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxWidth().weight(@Suppress("MagicNumber") 0.8f)) {
        ScrollableLazyColumn {
            item {
                Column(modifier = Modifier.padding(MaterialTheme.spacing.level4)) {
                    modelContent()
                }
            }
        }
    }

    Divider(Modifier.fillMaxWidth().height(2.dp), color = Color.Black)

    Box(Modifier.fillMaxWidth().weight(@Suppress("MagicNumber") 0.2f)) {
        ScrollableLazyColumn {
            item {
                ResultContent(result)
            }
        }
    }
}

@Composable
private
fun ResultContent(result: Result) {
    Column(modifier = Modifier.padding(MaterialTheme.spacing.level4)) {
        val syncResult = if (result.isSuccess) { "Success" } else { "Failure" }
        Text(text = "Sync Result: $syncResult", style = MaterialTheme.typography.titleMedium)
        result.list.let {
            Spacing.VerticalLevel4()
            Text(
                text = "Errors (${result.list.size} total)",
                style = MaterialTheme.typography.titleSmall
            )
            result.list.forEachIndexed { i, exception ->
                Text(text = "$i: Error: " + exception.message, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

data class Result(val isSuccess: Boolean, val list: List<Exception>)

@Composable
private
fun BoxScope.ScrollableLazyColumn(content: LazyListScope.() -> Unit) {
    val scrollState = rememberLazyListState()
    LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize()) {
        content()
    }
    VerticalScrollbar(
        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
        adapter = rememberScrollbarAdapter(
            scrollState = scrollState
        )
    )
}
