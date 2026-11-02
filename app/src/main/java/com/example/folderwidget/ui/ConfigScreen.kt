@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.folderwidget.ui

import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.folderwidget.R
import com.example.folderwidget.data.FolderConfig
import com.example.folderwidget.data.Preset
import com.example.folderwidget.data.PresetStore
import com.example.folderwidget.render.FolderRenderer
import com.example.folderwidget.shapes.AdaptiveShape
import com.example.folderwidget.shapes.FolderShapes
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val PRESET_COLORS = listOf(
    0xFF3FB8C9, 0xFF5B8DEF, 0xFF8E6CEF, 0xFFE0568B, 0xFFEF6C57,
    0xFFF2A93B, 0xFF4CB782, 0xFF2B2F36, 0xFFF1F3F5,
).map { it.toInt() }

private fun closeColor(a: Int, b: Int): Boolean =
    abs(AndroidColor.red(a) - AndroidColor.red(b)) <= 2 &&
        abs(AndroidColor.green(a) - AndroidColor.green(b)) <= 2 &&
        abs(AndroidColor.blue(a) - AndroidColor.blue(b)) <= 2

@Composable
fun ConfigScreen(
    initial: FolderConfig,
    widgetSizeDp: Pair<Float, Float>? = null,
    onSave: (FolderConfig) -> Unit,
) {
    val ctx = LocalContext.current

    var shapeId by remember { mutableStateOf(FolderShapes.byId(initial.shapeId).id) }
    val shape = FolderShapes.byId(shapeId)

    val initialHsv = remember { FloatArray(3).also { AndroidColor.colorToHSV(initial.color, it) } }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var sat by remember { mutableFloatStateOf(initialHsv[1]) }
    var bri by remember { mutableFloatStateOf(initialHsv[2]) }
    val color = AndroidColor.HSVToColor(floatArrayOf(hue, sat, bri))

    var transparency by remember { mutableIntStateOf(initial.transparency.coerceIn(0, 100)) }
    var roundness by remember { mutableIntStateOf(initial.roundness.coerceIn(0, 100)) }

    val selected = remember {
        mutableStateListOf<String>().apply { addAll(initial.apps.take(shape.maxSelectable)) }
    }

    // Saved presets: tap one to apply it here, or save the current look as a new one.
    var presets by remember { mutableStateOf(PresetStore.load(ctx)) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }
    var includeApps by remember { mutableStateOf(true) }

    fun applyPreset(p: Preset) {
        val s = FolderShapes.byId(p.shapeId)
        shapeId = s.id
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(p.color, hsv)
        hue = hsv[0]
        sat = hsv[1]
        bri = hsv[2]
        transparency = p.transparency.coerceIn(0, 100)
        roundness = p.roundness.coerceIn(0, 100)
        p.apps?.let { list ->
            selected.clear()
            selected.addAll(list.take(s.maxSelectable))
        }
    }

    var query by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf<List<AppInfo>?>(null) }
    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { loadLaunchableApps(ctx) }
    }

    val selectedSnapshot = selected.toList()
    // Resizable shapes are previewed with the real widget proportions.
    val realSize = if (shape is AdaptiveShape) widgetSizeDp else null
    val previewAspect = realSize?.let { it.first / it.second } ?: shape.previewAspectRatio
    val preview = remember(shapeId, color, selectedSnapshot, transparency, roundness, previewAspect) {
        val previewWidth = 640
        val previewHeight = (previewWidth / previewAspect).roundToInt().coerceIn(120, 1600)
        val pxPerDp = realSize?.let { previewWidth / it.first } ?: 2f
        FolderRenderer.render(
            ctx,
            FolderConfig(shapeId, color, selectedSnapshot, transparency, roundness),
            previewWidth,
            previewHeight,
            pxPerDp,
        ).asImageBitmap()
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(stringResource(R.string.save_preset)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = presetName,
                        onValueChange = { presetName = it },
                        label = { Text(stringResource(R.string.preset_name)) },
                        singleLine = true,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { includeApps = !includeApps },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = includeApps, onCheckedChange = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.preset_include_apps))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = presetName.trim().ifEmpty {
                            ctx.getString(R.string.preset_default_name, presets.size + 1)
                        }
                        val current = FolderConfig(shapeId, color, selected.toList(), transparency, roundness)
                        PresetStore.add(ctx, Preset.from(name, current, includeApps))
                        presets = PresetStore.load(ctx)
                        showSaveDialog = false
                        Toast.makeText(ctx, R.string.preset_saved, Toast.LENGTH_SHORT).show()
                    },
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        presetName = ""
                        includeApps = true
                        showSaveDialog = true
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.save_preset)) }
                Button(
                    onClick = { onSave(FolderConfig(shapeId, color, selected.toList(), transparency, roundness)) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.save)) }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(stringResource(R.string.config_title), style = MaterialTheme.typography.headlineSmall)
            }

            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val previewModifier = if (previewAspect > 1.3f) {
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .aspectRatio(previewAspect)
                    } else {
                        Modifier
                            .height(200.dp)
                            .aspectRatio(previewAspect)
                    }
                    // Faint frame so the widget area stays visible when the body is transparent.
                    Box(
                        modifier = previewModifier.border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            RoundedCornerShape(12.dp),
                        ),
                    ) {
                        Image(
                            bitmap = preview,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            // Saved presets
            if (presets.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.section_presets), style = MaterialTheme.typography.titleMedium)
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presets, key = { it.id }) { preset ->
                            FilterChip(
                                selected = false,
                                onClick = { applyPreset(preset) },
                                label = { Text(preset.name) },
                            )
                        }
                    }
                }
            }

            // Shape
            item {
                Text(stringResource(R.string.section_shape), style = MaterialTheme.typography.titleMedium)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FolderShapes.all, key = { it.id }) { s ->
                        FilterChip(
                            selected = s.id == shapeId,
                            onClick = {
                                shapeId = s.id
                                while (selected.size > s.maxSelectable) selected.removeAt(selected.lastIndex)
                            },
                            label = { Text(stringResource(s.nameRes)) },
                        )
                    }
                }
            }

            // Color
            item {
                Text(stringResource(R.string.section_color), style = MaterialTheme.typography.titleMedium)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(PRESET_COLORS) { preset ->
                        val isCurrent = closeColor(preset, color)
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(preset))
                                .border(
                                    width = if (isCurrent) 3.dp else 1.dp,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape,
                                )
                                .clickable {
                                    val hsv = FloatArray(3)
                                    AndroidColor.colorToHSV(preset, hsv)
                                    hue = hsv[0]
                                    sat = hsv[1]
                                    bri = hsv[2]
                                },
                        )
                    }
                }
            }
            item {
                Column {
                    SliderRow(stringResource(R.string.color_hue), hue, 0f..360f) { hue = it }
                    SliderRow(stringResource(R.string.color_saturation), sat, 0f..1f) { sat = it }
                    SliderRow(stringResource(R.string.color_brightness), bri, 0f..1f) { bri = it }
                    SliderRow(stringResource(R.string.color_transparency), transparency.toFloat(), 0f..100f) {
                        transparency = it.roundToInt()
                    }
                    if (shape.supportsRoundness) {
                        SliderRow(stringResource(R.string.shape_roundness), roundness.toFloat(), 0f..100f) {
                            roundness = it.roundToInt()
                        }
                        Text(
                            stringResource(R.string.roundness_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Apps
            item {
                Column {
                    Text(
                        stringResource(R.string.section_apps, selected.size, shape.maxSelectable),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    val hint = when {
                        shape is AdaptiveShape -> stringResource(R.string.grid_overflow_hint)
                        selected.size > shape.maxApps -> stringResource(R.string.count_overflow_hint, shape.maxApps)
                        else -> null
                    }
                    if (hint != null) {
                        Text(
                            hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.search_apps)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            val all = apps
            if (all == null) {
                item { CircularProgressIndicator() }
            } else {
                val filtered = all.filter { it.label.contains(query, ignoreCase = true) }
                items(filtered, key = { it.pkg }) { app ->
                    AppRow(
                        app = app,
                        checked = app.pkg in selected,
                        onClick = {
                            if (app.pkg in selected) {
                                selected.remove(app.pkg)
                            } else if (selected.size < shape.maxSelectable) {
                                selected.add(app.pkg)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    val percent = (((value - range.start) / (range.endInclusive - range.start)) * 100f).roundToInt()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(88.dp), style = MaterialTheme.typography.bodyMedium)
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f))
        Text(
            "$percent%",
            modifier = Modifier.width(48.dp),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun AppRow(app: AppInfo, checked: Boolean, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val icon = remember(app.pkg) {
        runCatching { ctx.packageManager.getApplicationIcon(app.pkg).toBitmap(96, 96).asImageBitmap() }
            .getOrNull()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(40.dp))
        } else {
            Spacer(Modifier.size(40.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(app.label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Checkbox(checked = checked, onCheckedChange = null)
    }
}
