@file:Suppress("TooManyFunctions")

package org.gradle.client.ui.composables

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text2.input.maxLengthInChars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.gradle.client.ui.theme.spacing
import org.gradle.client.ui.theme.transparency

@Composable
fun TitleLarge(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.titleLarge,
    )
}

@Composable
fun TitleMedium(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
fun TitleSmall(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.titleSmall,
    )
}

@Composable
fun TitleSmall(text: AnnotatedString, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.titleSmall,
    )
}

@Composable
fun BodyMedium(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
fun LabelSmall(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.labelSmall,
    )
}

@Composable
fun LabelMedium(text: String, textStyle: TextStyle = TextStyle.Default, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = text,
        style = textStyle.plus(MaterialTheme.typography.labelMedium),
    )
}

@Composable
fun HeadlineSmall(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.headlineSmall,
    )
}

@Composable
fun CodeBlock(
    modifier: Modifier = Modifier,
    code: AnnotatedString,
    popup: String?,
    onClick: (Int) -> Unit = {},
) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    Surface(
        tonalElevation = MaterialTheme.spacing.level1,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier.onSizeChanged { boxSize = it }
    ) {
        Box {
            ClickableText(
                text = code,
                modifier = Modifier.padding(MaterialTheme.spacing.level2),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                onClick = onClick,
            )
            Box(
                modifier =
                    Modifier
                        .padding(bottom = 8.dp)
                        .background(MaterialTheme.colorScheme.background)
                        .width(boxSize.width.toDp() - 8.dp)
                        .align(Alignment.BottomCenter)
                        .shadow(2.dp)
                        .animateContentSize()
            ) {
                if (popup != null) {
                    Text(
                        text = popup,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun Int.toDp() = with(LocalDensity.current) { this@toDp.toDp() }

fun Modifier.semiTransparentIfNull(any: Any?) =
    if (any == null) alpha(MaterialTheme.transparency.HALF) else this
