package com.example.folderwidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.folderwidget.data.FolderConfig
import com.example.folderwidget.data.FolderPrefs
import com.example.folderwidget.ui.ConfigScreen
import com.example.folderwidget.ui.FolderTheme

class ConfigActivity : ComponentActivity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If the user backs out, the widget must not be added.
        setResult(RESULT_CANCELED)

        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val initial = FolderPrefs.load(this, widgetId)
        setContent {
            FolderTheme {
                ConfigScreen(initial = initial, onSave = ::save)
            }
        }
    }

    private fun save(cfg: FolderConfig) {
        FolderPrefs.save(this, widgetId, cfg)
        FolderWidgetProvider.update(this, AppWidgetManager.getInstance(this), widgetId)
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }
}
