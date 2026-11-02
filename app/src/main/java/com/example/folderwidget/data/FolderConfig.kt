package com.example.folderwidget.data

import android.content.Context
import androidx.core.content.edit

/** Everything the user can customize for one widget instance. */
data class FolderConfig(
    val shapeId: String = "flower",
    val color: Int = 0xFF3FB8C9.toInt(),
    /** Package names, in display order. How many fit on the widget is decided by its size. */
    val apps: List<String> = emptyList(),
    /** 0 = fully visible, 100 = completely invisible. */
    val transparency: Int = 0,
    /** 0 = normal outline, 100 = circle (only for shapes that support it, like the flower). */
    val roundness: Int = 0,
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
            apps = p.getString("apps_$widgetId", "").orEmpty()
                .split(",")
                .filter { it.isNotBlank() },
            transparency = p.getInt("transparency_$widgetId", d.transparency).coerceIn(0, 100),
            roundness = p.getInt("roundness_$widgetId", d.roundness).coerceIn(0, 100),
        )
    }

    fun save(ctx: Context, widgetId: Int, cfg: FolderConfig) {
        prefs(ctx).edit {
            putString("shape_$widgetId", cfg.shapeId)
            putInt("color_$widgetId", cfg.color)
            putString("apps_$widgetId", cfg.apps.joinToString(","))
            putInt("transparency_$widgetId", cfg.transparency.coerceIn(0, 100))
            putInt("roundness_$widgetId", cfg.roundness.coerceIn(0, 100))
            remove("count_$widgetId") // legacy key from older versions
        }
    }

    fun delete(ctx: Context, widgetId: Int) {
        prefs(ctx).edit {
            remove("shape_$widgetId")
            remove("color_$widgetId")
            remove("count_$widgetId")
            remove("apps_$widgetId")
            remove("transparency_$widgetId")
            remove("roundness_$widgetId")
        }
    }

    /** True once the widget has been configured (saved) at least once. */
    fun has(ctx: Context, widgetId: Int): Boolean = prefs(ctx).contains("shape_$widgetId")
}
