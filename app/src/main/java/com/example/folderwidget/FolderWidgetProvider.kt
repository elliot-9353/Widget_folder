package com.example.folderwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.example.folderwidget.data.FolderConfig
import com.example.folderwidget.data.FolderPrefs
import com.example.folderwidget.render.FolderRenderer
import com.example.folderwidget.shapes.AdaptiveShape
import com.example.folderwidget.shapes.FolderShapes
import com.example.folderwidget.shapes.GridShape
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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
        /** Must match the number of rows/columns in res/layout/widget_folder.xml. */
        private const val GRID = 10

        /** Broadcast action of the click that does nothing (see [noopPending]). */
        private const val ACTION_NOOP = "com.example.folderwidget.NOOP"

        /** Current widget size in dp (width, height) for the current orientation. */
        fun sizeDp(context: Context, manager: AppWidgetManager, widgetId: Int): Pair<Float, Float> {
            val options = manager.getAppWidgetOptions(widgetId)
            val portrait = context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
            val w = options.getInt(
                if (portrait) AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
                else AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
                110,
            )
            val h = options.getInt(
                if (portrait) AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
                else AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                110,
            )
            return Pair(w.toFloat(), h.toFloat())
        }

        fun update(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val (wDp, hDp) = sizeDp(context, manager, widgetId)
            val density = context.resources.displayMetrics.density
            val cfg = FolderPrefs.load(context, widgetId)

            if (FolderShapes.byId(cfg.shapeId) is AdaptiveShape) {
                updateGrid(context, manager, widgetId, cfg, wDp, hDp, density)
                return
            }

            // Classic shapes: one picture, capped in size.
            var wf = wDp * density
            var hf = hDp * density
            val scale = min(1f, 512f / max(wf, hf))
            wf *= scale
            hf *= scale
            val wPx = wf.toInt().coerceAtLeast(64)
            val hPx = hf.toInt().coerceAtLeast(64)
            val bitmap = FolderRenderer.render(context, cfg, wPx, hPx)
            updateClassic(context, manager, widgetId, cfg, bitmap, wPx, hPx)
        }

        private fun appPending(context: Context, widgetId: Int, index: Int, pkg: String): PendingIntent? =
            context.packageManager.getLaunchIntentForPackage(pkg)?.let { launch ->
                PendingIntent.getActivity(
                    context,
                    widgetId * 100 + index,
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }

        /** Opens the full list of apps. */
        private fun popupPending(context: Context, widgetId: Int): PendingIntent {
            val popup = Intent(context, PopupActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                .setData(Uri.parse("folderwidget://popup/$widgetId"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return PendingIntent.getActivity(
                context,
                widgetId * 100 + 99,
                popup,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        /**
         * A click that does nothing. The launcher re-applies each update on top of the views it
         * already has, so a cell that is simply left alone keeps the click it got in an OLDER
         * update (for example the app it held before the widget was resized). Every cell must
         * therefore get a click action on every update; empty cells get this one.
         */
        private fun noopPending(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, FolderWidgetProvider::class.java).setAction(ACTION_NOOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        private fun idOf(context: Context, name: String): Int =
            context.resources.getIdentifier(name, "id", context.packageName)

        /**
         * Resizable grid. Real fixed-size cells (hidden when unused) carry the tap targets; the
         * folder body and the icon picture are stretched over exactly those cells by the layout, so
         * they always line up and the body never exceeds the cells (no wasted space).
         * Only the number of columns/rows depends on the size reported by the launcher.
         */
        private fun updateGrid(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            cfg: FolderConfig,
            wDp: Float,
            hDp: Float,
            density: Float,
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_grid)

            // 0% = opaque body, 100% = invisible body (icons are never affected).
            val alpha = 255 * (100 - cfg.transparency.coerceIn(0, 100)) / 100
            views.setInt(R.id.widget_body, "setColorFilter", cfg.color)
            views.setInt(R.id.widget_body, "setImageAlpha", alpha)

            val pkgs = FolderRenderer.visibleApps(cfg)
            val (cols, rows) = GridShape.fit(wDp, hDp)
            // One cell is PITCH_DP wide in the layout; the system rounds it to whole pixels, so do the same.
            val pitchPx = (GridShape.PITCH_DP * density + 0.5f).toInt()
            val latticeW = (cols * pitchPx).toFloat()
            val latticeH = (rows * pitchPx).toFloat()
            val plan = GridShape.layout(cols, rows, pkgs.size, latticeW, latticeH, density)
            val icons = FolderRenderer.renderGridIcons(
                context, cfg, plan, latticeW.roundToInt(), latticeH.roundToInt(),
            )
            views.setImageViewBitmap(R.id.widget_image, icons)

            val popup = popupPending(context, widgetId)
            val noop = noopPending(context)

            for (r in 0 until GridShape.MAX_ROWS) {
                val rowId = idOf(context, "grow_$r")
                if (rowId != 0) {
                    views.setViewVisibility(rowId, if (r < rows) View.VISIBLE else View.GONE)
                }
                for (c in 0 until GridShape.MAX_COLS) {
                    val cellId = idOf(context, "gcell_${r}_$c")
                    if (cellId == 0) continue
                    val used = r < rows && c < cols
                    views.setViewVisibility(cellId, if (used) View.VISIBLE else View.GONE)

                    // Every cell gets a click action, used or not, so nothing stale survives.
                    val index = r * cols + c
                    val pending = when {
                        !used -> noop
                        index < plan.shown -> appPending(context, widgetId, index, pkgs[index]) ?: noop
                        plan.showMore && index == plan.shown -> popup
                        else -> noop
                    }
                    views.setOnClickPendingIntent(cellId, pending)
                }
            }
            manager.updateAppWidget(widgetId, views)
        }

        /** Flower / horizontal: a picture plus an invisible GRID x GRID layer of tap cells. */
        private fun updateClassic(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            cfg: FolderConfig,
            bitmap: android.graphics.Bitmap,
            wPx: Int,
            hPx: Int,
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_folder)
            views.setImageViewBitmap(R.id.widget_image, bitmap)

            // One pending intent per app (opens that app directly).
            val layout = FolderRenderer.layoutOf(cfg, wPx, hPx)
            val appIntents = layout.pkgs.mapIndexed { index, pkg -> appPending(context, widgetId, index, pkg) }

            // The center mark of shapes that have one opens the full app list.
            val popupPending = popupPending(context, widgetId)
            val noop = noopPending(context)

            // Invisible GRID x GRID cells are mapped to the nearest rendered icon.
            for (row in 0 until GRID) for (column in 0 until GRID) {
                val id = idOf(context, "cell_${row}_$column")
                if (id == 0) continue
                val x = (column + 0.5f) / GRID * wPx
                val y = (row + 0.5f) / GRID * hPx

                var best = -1
                var bestDistance = Float.MAX_VALUE
                layout.centers.forEachIndexed { index, point ->
                    val distance = hypot(x - point.x, y - point.y)
                    if (distance < bestDistance) {
                        bestDistance = distance
                        best = index
                    }
                }

                val pending = when {
                    best >= 0 && bestDistance <= layout.iconSize * 0.7f -> appIntents[best]
                    layout.showCenterMark &&
                        hypot(x - layout.cx, y - layout.cy) <= layout.size * 0.1f -> popupPending
                    else -> null
                }
                // Always set something: see noopPending().
                views.setOnClickPendingIntent(id, pending ?: noop)
            }

            manager.updateAppWidget(widgetId, views)
        }
    }
}
