package com.example.folderwidget.data

import android.content.Context
import androidx.core.content.edit

/** Everything the user can customize for one widget instance. */
data class FolderConfig(
    val shapeId: String = "flower",
    val color: Int = 0xFF3FB8C9.toInt(),
    /** Maximum number of apps in the folder. */
    val count: Int = 6,
    /** Package names, in display order. */
    val apps: List<String> = emptyList(),
)

/** Stores one [FolderConfig] per widget id. */
object FolderPrefs {
    private const val FILE = "folder_widget_prefs"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(ctx: Context, widgetId: Int): FolderConfig {
        val p = prefs(ctx)
        val d = FolderConfig()
        return FolderConfig(
            shapeId = p.getString("shape_$widgetId", d.shapeId) ?: d.shapeId,
            color = p.getInt("color_$widgetId", d.color),
            count = p.getInt("count_$widgetId", d.count),
            apps = p.getString("apps_$widgetId", "").orEmpty()
                .split(",")
                .filter { it.isNotBlank() },
        )
    }

    fun save(ctx: Context, widgetId: Int, cfg: FolderConfig) {
        prefs(ctx).edit {
            putString("shape_$widgetId", cfg.shapeId)
            putInt("color_$widgetId", cfg.color)
            putInt("count_$widgetId", cfg.count)
            putString("apps_$widgetId", cfg.apps.joinToString(","))
        }
    }

    fun delete(ctx: Context, widgetId: Int) {
        prefs(ctx).edit {
            remove("shape_$widgetId")
            remove("color_$widgetId")
            remove("count_$widgetId")
            remove("apps_$widgetId")
        }
    }
}
