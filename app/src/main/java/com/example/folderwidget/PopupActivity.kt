package com.example.folderwidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.example.folderwidget.data.FolderPrefs
import com.example.folderwidget.shapes.FolderShapes
import com.example.folderwidget.ui.FolderTheme
import com.example.folderwidget.ui.PopupApp
import com.example.folderwidget.ui.PopupScreen

/** Transparent window shown when the widget is tapped: lists every app of the folder. */
class PopupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        val cfg = FolderPrefs.load(this, widgetId)
        val shape = FolderShapes.byId(cfg.shapeId)
        val pm = packageManager
        val iconPx = (56 * resources.displayMetrics.density).toInt()

        val apps = cfg.apps
            .take(cfg.count.coerceIn(shape.minApps, shape.maxApps))
            .mapNotNull { pkg ->
                if (pm.getLaunchIntentForPackage(pkg) == null) return@mapNotNull null
                val label = runCatching {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                }.getOrDefault(pkg)
                val icon = runCatching {
                    pm.getApplicationIcon(pkg).toBitmap(iconPx, iconPx).asImageBitmap()
                }.getOrNull()
                PopupApp(pkg, label, icon)
            }

        setContent {
            FolderTheme {
                PopupScreen(
                    apps = apps,
                    color = cfg.color,
                    onClose = { finish() },
                    onLaunch = { app ->
                        pm.getLaunchIntentForPackage(app.pkg)?.let {
                            startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                        finish()
                    },
                )
            }
        }
    }
}
