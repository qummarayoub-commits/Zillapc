package com.darkjade.streamlib.ui.screens.player

import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.util.TypedValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView

private const val PREFS_NAME = "player_display_settings"

@OptIn(UnstableApi::class)
@Composable
fun PlayerDisplaySettingsDialog(
    playerView: PlayerView?,
    onDismiss: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE) }

    var screenMode by remember { mutableStateOf(prefs.getString("screen_mode", "Fit") ?: "Fit") }
    var fontSize by remember { mutableStateOf(prefs.getInt("subtitle_size", 22).toFloat()) }
    var fontName by remember { mutableStateOf(prefs.getString("subtitle_font", "Sans-serif") ?: "Sans-serif") }
    var fontStyle by remember { mutableStateOf(prefs.getString("subtitle_font_style", "Regular") ?: "Regular") }
    var fontColor by remember { mutableStateOf(prefs.getString("subtitle_color", "White") ?: "White") }
    var edgeStyle by remember { mutableStateOf(prefs.getString("subtitle_edge", "Outline") ?: "Outline") }
    var backgroundStyle by remember { mutableStateOf(prefs.getString("subtitle_background", "Transparent") ?: "Transparent") }
    var bottomPosition by remember { mutableStateOf(prefs.getFloat("subtitle_position", 0.08f)) }

    fun applySettings() {
        prefs.edit()
            .putString("screen_mode", screenMode)
            .putInt("subtitle_size", fontSize.toInt())
            .putString("subtitle_font", fontName)
            .putString("subtitle_font_style", fontStyle)
            .putString("subtitle_color", fontColor)
            .putString("subtitle_edge", edgeStyle)
            .putString("subtitle_background", backgroundStyle)
            .putFloat("subtitle_position", bottomPosition)
            .apply()

        playerView?.resizeMode = when (screenMode) {
            "Crop" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            "Stretch" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            "Fixed width" -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
            "Fixed height" -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }

        val foreground = when (fontColor) {
            "Yellow" -> AndroidColor.YELLOW
            "Cyan" -> AndroidColor.CYAN
            "Green" -> AndroidColor.GREEN
            "Magenta" -> AndroidColor.MAGENTA
            "Red" -> AndroidColor.RED
            else -> AndroidColor.WHITE
        }
        val background = when (backgroundStyle) {
            "Black box" -> AndroidColor.BLACK
            "Dark translucent" -> AndroidColor.argb(160, 0, 0, 0)
            else -> AndroidColor.TRANSPARENT
        }
        val edgeType = when (edgeStyle) {
            "Shadow" -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
            "Raised" -> CaptionStyleCompat.EDGE_TYPE_RAISED
            "Depressed" -> CaptionStyleCompat.EDGE_TYPE_DEPRESSED
            "None" -> CaptionStyleCompat.EDGE_TYPE_NONE
            else -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
        }
        val typeface = when (fontName) {
            "Serif" -> "serif"
            "Monospace" -> "monospace"
            "Casual" -> "casual"
            "Cursive" -> "cursive"
            else -> "sans-serif"
        }
        playerView?.subtitleView?.apply {
            setStyle(CaptionStyleCompat(
                foreground,
                background,
                AndroidColor.TRANSPARENT,
                edgeType,
                AndroidColor.BLACK,
                Typeface.create(typeface, when (fontStyle) { "Bold" -> Typeface.BOLD; "Italic" -> Typeface.ITALIC; "Bold italic" -> Typeface.BOLD_ITALIC; else -> Typeface.NORMAL }),
            ))
            setFixedTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, fontSize, context.resources.displayMetrics))
            setBottomPaddingFraction(bottomPosition)
        }
    }

    LaunchedEffect(playerView, screenMode, fontSize, fontName, fontStyle, fontColor, edgeStyle, backgroundStyle, bottomPosition) {
        applySettings()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Screen & Subtitle Settings") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Screen size", style = MaterialTheme.typography.titleSmall)
                ChoiceDropdown(
                    label = "Scaling mode",
                    selected = screenMode,
                    options = listOf("Fit", "Crop", "Stretch", "Fixed width", "Fixed height"),
                    onSelect = { screenMode = it },
                )
                Text("Fit keeps the full picture. Crop fills the screen and may cut off edges. Stretch can distort the picture.", style = MaterialTheme.typography.bodySmall)

                Text("Subtitle appearance", style = MaterialTheme.typography.titleSmall)
                ChoiceDropdown("Font type", fontName, listOf("Sans-serif", "Serif", "Monospace", "Casual", "Cursive")) { fontName = it }
                ChoiceDropdown("Font style", fontStyle, listOf("Regular", "Bold", "Italic", "Bold italic")) { fontStyle = it }
                ChoiceDropdown("Font color", fontColor, listOf("White", "Yellow", "Cyan", "Green", "Magenta", "Red")) { fontColor = it }
                Text("Font size: ${fontSize.toInt()} sp", style = MaterialTheme.typography.bodyMedium)
                Slider(value = fontSize, onValueChange = { fontSize = it }, valueRange = 14f..40f)
                ChoiceDropdown("Text outline", edgeStyle, listOf("Outline", "Shadow", "Raised", "Depressed", "None")) { edgeStyle = it }
                ChoiceDropdown("Text background", backgroundStyle, listOf("Transparent", "Black box", "Dark translucent")) { backgroundStyle = it }
                Text("Subtitle vertical position", style = MaterialTheme.typography.bodyMedium)
                Slider(value = bottomPosition, onValueChange = { bottomPosition = it }, valueRange = 0.02f..0.25f)
                Text("Lower to keep subtitles near the bottom; raise to move them up.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = {
                screenMode = "Fit"
                fontSize = 22f
                fontName = "Sans-serif"
                fontStyle = "Regular"
                fontColor = "White"
                edgeStyle = "Outline"
                backgroundStyle = "Transparent"
                bottomPosition = 0.08f
            }) { Text("Reset") }
        },
    )
}

@Composable
private fun ChoiceDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall)
        androidx.compose.material3.TextButton(onClick = { expanded = true }) {
            Text(selected)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
