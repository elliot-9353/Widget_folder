package com.example.folderwidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.folderwidget.data.Preset
import com.example.folderwidget.data.PresetStore
import com.example.folderwidget.ui.FolderTheme
import com.example.folderwidget.ui.MainScreen

class MainActivity : ComponentActivity() {
    private var presets by mutableStateOf<List<Preset>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presets = PresetStore.load(this)
        setContent {
            FolderTheme {
                MainScreen(
                    presets = presets,
                    onAddPlain = { pinWidget(null) },
                    onAddPreset = { pinWidget(it) },
                    onDelete = {
                        PresetStore.delete(this, it.id)
                        presets = PresetStore.load(this)
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Presets are saved from the widget settings, so refresh when coming back.
        presets = PresetStore.load(this)
    }

    /** Asks the launcher to add the widget; with a [preset], the new widget starts from it. */
    private fun pinWidget(preset: Preset?) {
        val manager = AppWidgetManager.getInstance(this)
        if (!manager.isRequestPinAppWidgetSupported) {
            Toast.makeText(this, R.string.pin_unsupported, Toast.LENGTH_LONG).show()
            return
        }
        if (preset != null) PresetStore.setPending(this, preset.id) else PresetStore.clearPending(this)
        manager.requestPinAppWidget(ComponentName(this, FolderWidgetProvider::class.java), null, null)
    }
}
