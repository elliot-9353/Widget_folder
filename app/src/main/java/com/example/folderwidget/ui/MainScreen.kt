package com.example.folderwidget.ui

import android.content.Context
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.folderwidget.R
import com.example.folderwidget.data.FolderConfig
import com.example.folderwidget.data.Preset
import com.example.folderwidget.render.FolderRenderer
import com.example.folderwidget.shapes.AdaptiveShape
import com.example.folderwidget.shapes.FolderShapes
import kotlin.math.roundToInt

/** Home screen of the app: how to add the widget, and the presets saved from the widget settings. */
@Composable
fun MainScreen(
    presets: List<Preset>,
    onAddPlain: () -> Unit,
    onAddPreset: (Preset) -> Unit,
    onDelete: (Preset) -> Unit,
) {
    var toDelete by remember { mutableStateOf<Preset?>(null) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { Text(stringResource(R.string.main_title), style = MaterialTheme.typography.headlineMedium) }
            item {
                Text(
                    stringResource(R.string.main_steps),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            item { Button(onClick = onAddPlain) { Text(stringResource(R.string.add_widget)) } }

            item {
                Text(
                    stringResource(R.string.presets_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
            }
            if (presets.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.presets_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(presets, key = { it.id }) { preset ->
                    PresetCard(preset, onAdd = { onAddPreset(preset) }, onDelete = { toDelete = preset })
                }
            }
        }
    }

    toDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.delete_preset_title)) },
            text = { Text(stringResource(R.string.delete_preset_message, preset.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(preset)
                        toDelete = null
                    },
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun PresetCard(preset: Preset, onAdd: () -> Unit, onDelete: () -> Unit) {
    val ctx = LocalContext.current
    val shape = FolderShapes.byId(preset.shapeId)
    val preview = remember(preset) { previewOf(ctx, preset) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(width = 110.dp, height = 90.dp), contentAlignment = Alignment.Center) {
                Image(
                    bitmap = preview,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    preset.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val apps = preset.apps
                val shapeName = stringResource(shape.nameRes)
                Text(
                    if (apps == null) {
                        stringResource(R.string.preset_info_no_apps, shapeName)
                    } else {
                        stringResource(R.string.preset_info_apps, shapeName, apps.size)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row {
                    TextButton(onClick = onAdd) { Text(stringResource(R.string.add_to_home)) }
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete)) }
                }
            }
        }
    }
}

/** A small picture of the preset, drawn like the widget (resizable shapes use a nominal 2x2 widget). */
private fun previewOf(ctx: Context, preset: Preset): ImageBitmap {
    val cfg: FolderConfig = preset.applyTo(FolderConfig())
    val shape = FolderShapes.byId(cfg.shapeId)
    val widthPx = 360
    val aspect = if (shape is AdaptiveShape) 1f else shape.previewAspectRatio
    val pxPerDp = if (shape is AdaptiveShape) widthPx / 150f else 2f
    val heightPx = (widthPx / aspect).roundToInt().coerceAtLeast(60)
    return FolderRenderer.render(ctx, cfg, widthPx, heightPx, pxPerDp).asImageBitmap()
}
