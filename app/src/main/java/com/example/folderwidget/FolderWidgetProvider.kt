package com.example.folderwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import com.example.folderwidget.data.FolderPrefs
import com.example.folderwidget.render.FolderRenderer
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class FolderWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { update(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        update(context, manager, appWidgetId)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { FolderPrefs.delete(context, it) }
    }

    companion object {
        /** Must match the number of rows/columns in res/layout/widget_folder.xml */
        private const val GRID = 10

        fun update(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val options = manager.getAppWidgetOptions(widgetId)
            val portrait = context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
            val wDp = options.getInt(
                if (portrait) AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
                else AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
                110,
            )
            val hDp = options.getInt(
                if (portrait) AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
                else AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                110,
            )
            val density = context.resources.displayMetrics.density
            var wf = wDp * density
            var hf = hDp * density
            val k = min(1f, 512f / max(wf, hf))
            wf *= k
            hf *= k
            val wPx = wf.toInt().coerceAtLeast(64)
            val hPx = hf.toInt().coerceAtLeast(64)

            val cfg = FolderPrefs.load(context, widgetId)
            val bitmap = FolderRenderer.render(context, cfg, wPx, hPx)

            val views = RemoteViews(context.packageName, R.layout.widget_folder)
            views.setImageViewBitmap(R.id.widget_image, bitmap)

            // One pending intent per app (opens the app directly).
            val layout = FolderRenderer.layoutOf(cfg, wPx, hPx)
            val pm = context.packageManager
            val appIntents = layout.pkgs.mapIndexed { i, pkg ->
                pm.getLaunchIntentForPackage(pkg)?.let { launch ->
                    PendingIntent.getActivity(
                        context,
                        widgetId * 100 + i,
                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                }
            }

            // Tapping the center dots still opens the full list.
            val popup = Intent(context, PopupActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                .setData(Uri.parse("folderwidget://popup/$widgetId"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val popupPending = PendingIntent.getActivity(
                context,
                widgetId * 100 + 99,
                popup,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            // The widget has an invisible GRID x GRID layer of tap cells on top of the image.
            // Each cell opens the nearest icon if it falls on it.
            for (r in 0 until GRID) for (c in 0 until GRID) {
                val id = context.resources.getIdentifier("cell_${r}_$c", "id", context.packageName)
                if (id == 0) continue
                val x = (c + 0.5f) / GRID * wPx
                val y = (r + 0.5f) / GRID * hPx

                var best = -1
                var bestDist = Float.MAX_VALUE
                layout.centers.forEachIndexed { i, p ->
                    val d = hypot(x - p.x, y - p.y)
                    if (d < bestDist) {
                        bestDist = d
                        best = i
                    }
                }

                val pending = when {
                    best >= 0 && bestDist <= layout.iconSize * 0.7f -> appIntents[best]
                    hypot(x - layout.cx, y - layout.cy) <= layout.size * 0.1f -> popupPending
                    else -> null
                }
                if (pending != null) views.setOnClickPendingIntent(id, pending)
            }

            manager.updateAppWidget(widgetId, views)
        }
    }
}
