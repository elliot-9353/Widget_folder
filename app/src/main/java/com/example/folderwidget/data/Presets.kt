package com.example.folderwidget.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** A saved look (and optionally the apps) that can be applied to any widget. */
data class Preset(
    val id: String,
    val name: String,
    val shapeId: String,
    val color: Int,
    val transparency: Int,
    val roundness: Int,
    /** Null = the preset was saved without its apps. */
    val apps: List<String>?,
) {
    /** [base] with this preset applied on top (the apps of [base] are kept when the preset has none). */
    fun applyTo(base: FolderConfig = FolderConfig()): FolderConfig = base.copy(
        shapeId = shapeId,
        color = color,
        transparency = transparency,
        roundness = roundness,
        apps = apps ?: base.apps,
    )

    companion object {
        fun from(name: String, cfg: FolderConfig, includeApps: Boolean) = Preset(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            shapeId = cfg.shapeId,
            color = cfg.color,
            transparency = cfg.transparency,
            roundness = cfg.roundness,
            apps = if (includeApps) cfg.apps else null,
        )
    }
}

/** Saved presets, kept as JSON inside the app (they survive widgets being removed). */
object PresetStore {
    private const val FILE = "folder_widget_presets"
    private const val KEY = "presets"
    private const val PENDING_ID = "pending_id"
    private const val PENDING_AT = "pending_at"
    private const val PENDING_TTL_MS = 3 * 60 * 1000L

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(ctx: Context): List<Preset> {
        val raw = prefs(ctx).getString(KEY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { fromJson(array.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun add(ctx: Context, preset: Preset) = write(ctx, load(ctx) + preset)

    fun delete(ctx: Context, id: String) = write(ctx, load(ctx).filterNot { it.id == id })

    /**
     * "Add this preset to the home screen": the next widget that gets configured (within a few
     * minutes) starts from this preset instead of the defaults.
     */
    fun setPending(ctx: Context, id: String) {
        prefs(ctx).edit {
            putString(PENDING_ID, id)
            putLong(PENDING_AT, System.currentTimeMillis())
        }
    }

    fun clearPending(ctx: Context) {
        prefs(ctx).edit {
            remove(PENDING_ID)
            remove(PENDING_AT)
        }
    }

    /** The preset waiting for a new widget, if any. It is used once. */
    fun takePending(ctx: Context): Preset? {
        val p = prefs(ctx)
        val id = p.getString(PENDING_ID, null) ?: return null
        val at = p.getLong(PENDING_AT, 0L)
        clearPending(ctx)
        if (System.currentTimeMillis() - at > PENDING_TTL_MS) return null
        return load(ctx).firstOrNull { it.id == id }
    }

    private fun write(ctx: Context, list: List<Preset>) {
        val array = JSONArray()
        list.forEach { array.put(toJson(it)) }
        prefs(ctx).edit { putString(KEY, array.toString()) }
    }

    private fun toJson(p: Preset): JSONObject {
        val o = JSONObject()
        o.put("id", p.id)
        o.put("name", p.name)
        o.put("shape", p.shapeId)
        o.put("color", p.color)
        o.put("transparency", p.transparency)
        o.put("roundness", p.roundness)
        p.apps?.let { apps -> o.put("apps", JSONArray(apps)) }
        return o
    }

    private fun fromJson(o: JSONObject): Preset = Preset(
        id = o.getString("id"),
        name = o.optString("name"),
        shapeId = o.optString("shape", "flower"),
        color = o.optInt("color", 0xFF3FB8C9.toInt()),
        transparency = o.optInt("transparency", 0),
        roundness = o.optInt("roundness", 0),
        apps = o.optJSONArray("apps")?.let { a -> (0 until a.length()).map { a.getString(it) } },
    )
}
