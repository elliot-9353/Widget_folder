@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.folderwidget.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.folderwidget.R
import com.example.folderwidget.data.FolderConfig
import com.example.folderwidget.render.FolderRenderer
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
fun ConfigScreen(initial: FolderConfig, onSave: (FolderConfig) -> Unit) {
    val ctx = LocalContext.current

    var shapeId by remember { mutableStateOf(FolderShapes.byId(initial.shapeId).id) }
    val shape = FolderShapes.byId(shapeId)

    val initialHsv = remember { FloatArray(3).also { AndroidColor.colorToHSV(initial.color, it) } }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var sat by remember { mutableFloatStateOf(initialHsv[1]) }
    var bri by remember { mutableFloatStateOf(initialHsv[2]) }
    val color = AndroidColor.HSVToColor(floatArrayOf(hue, sat, bri))

    var count by remember { mutableIntStateOf(initial.count.coerceIn(shape.minApps, shape.maxApps)) }
    val selected = remember {
        mutableStateListOf<String>().apply { addAll(initial.apps.take(initial.count)) }
    }

    var query by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf<List<AppInfo>?>(null) }
    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { loadLaunchableApps(ctx) }
    }

    val selectedSnapshot = selected.toList()
    val preview = remember(shapeId, color, count, selectedSnapshot) {
        FolderRenderer.render(ctx, FolderConfig(shapeId, color, count, selectedSnapshot), 480)
            .asImageBitmap()
    }

    Scaffold(
        bottomBar = {
            Button(
                onClick = { onSave(FolderConfig(shapeId, color, count, selected.toList())) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) { Text(stringResource(R.string.save)) }
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
                    Image(bitmap = preview, contentDescription = null, modifier = Modifier.size(200.dp))
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
                                count = count.coerceIn(s.minApps, s.maxApps)
                                while (selected.size > count) selected.removeAt(selected.lastIndex)
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
                }
            }

            // Number of apps
            item {
                Text(
                    stringResource(R.string.section_count, count),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            item {
                Slider(
                    value = count.toFloat(),
                    onValueChange = {
                        count = it.roundToInt()
                        while (selected.size > count) selected.removeAt(selected.lastIndex)
                    },
                    valueRange = shape.minApps.toFloat()..shape.maxApps.toFloat(),
                    steps = (shape.maxApps - shape.minApps - 1).coerceAtLeast(0),
                )
            }

            // Apps
            item {
                Text(
                    stringResource(R.string.section_apps, selected.size, count),
                    style = MaterialTheme.typography.titleMedium,
                )
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
                            } else if (selected.size < count) {
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(96.dp), style = MaterialTheme.typography.bodyMedium)
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f))
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
