package com.example.folderwidget.render

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import androidx.core.graphics.ColorUtils
import com.example.folderwidget.data.FolderConfig
import com.example.folderwidget.shapes.FolderShapes
import kotlin.math.min
import kotlin.math.roundToInt

/** Draws a folder (shape + app icons + center mark) into a square bitmap. */
object FolderRenderer {
    private val iconCache = HashMap<String, Bitmap>()

    /** Where the icons are, in bitmap pixels. Used for tap targets. */
    class Layout(
        val pkgs: List<String>,
        val centers: List<PointF>,
        val iconSize: Float,
        val cx: Float,
        val cy: Float,
        val size: Float,
    )

    fun layoutOf(cfg: FolderConfig, widthPx: Int, heightPx: Int): Layout {
        val size = min(widthPx, heightPx).toFloat()
        val offX = (widthPx - size) / 2f
        val offY = (heightPx - size) / 2f
        val shape = FolderShapes.byId(cfg.shapeId)
        val pkgs = cfg.apps.take(cfg.count.coerceIn(shape.minApps, shape.maxApps))
        val slotCount = pkgs.size.coerceAtLeast(shape.minApps)
        val centers = shape.slots(slotCount, size)
            .take(pkgs.size)
            .map { PointF(it.x + offX, it.y + offY) }
        return Layout(
            pkgs = pkgs,
            centers = centers,
            iconSize = shape.iconSize(slotCount, size),
            cx = offX + size / 2f,
            cy = offY + size / 2f,
            size = size,
        )
    }

    /** Square folder centered in a [widthPx] x [heightPx] bitmap. */
    fun render(ctx: Context, cfg: FolderConfig, widthPx: Int, heightPx: Int = widthPx): Bitmap {
        val size = min(widthPx, heightPx).toFloat()
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.translate((widthPx - size) / 2f, (heightPx - size) / 2f)

        val shape = FolderShapes.byId(cfg.shapeId)
        val limit = cfg.count.coerceIn(shape.minApps, shape.maxApps)
        val pkgs = cfg.apps.take(limit)
        val slotCount = pkgs.size.coerceAtLeast(shape.minApps)

        // Body
        val outline = shape.outline(slotCount, size)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = cfg.color
        }
        canvas.drawPath(outline, fill)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = size * 0.012f
            color = ColorUtils.blendARGB(cfg.color, Color.BLACK, 0.3f)
        }
        canvas.drawPath(outline, stroke)

        // Icons
        val iconSize = shape.iconSize(slotCount, size)
        val slots = shape.slots(slotCount, size)
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        val half = iconSize / 2f
        pkgs.forEachIndexed { i, pkg ->
            val c = slots.getOrNull(i) ?: return@forEachIndexed
            val icon = icon(ctx, pkg, iconSize.roundToInt()) ?: return@forEachIndexed
            canvas.drawBitmap(icon, null, RectF(c.x - half, c.y - half, c.x + half, c.y + half), iconPaint)
        }

        // Center mark (4 dots)
        if (shape.showCenterMark) {
            val light = ColorUtils.calculateLuminance(cfg.color) > 0.5
            val dots = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (light) 0x73000000 else 0xB3FFFFFF.toInt()
            }
            val c = size / 2f
            val off = size * 0.032f
            val r = size * 0.014f
            for (dx in listOf(-off, off)) for (dy in listOf(-off, off)) {
                canvas.drawCircle(c + dx, c + dy, r, dots)
            }
        }
        return bmp
    }

    /** App icon clipped to an anti-aliased rounded square. Cached per package and size. */
    private fun icon(ctx: Context, pkg: String, px: Int): Bitmap? {
        if (px <= 0) return null
        val key = "$pkg@$px"
        iconCache[key]?.let { return it }

        val drawable = try {
            ctx.packageManager.getApplicationIcon(pkg)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }

        val out = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        drawable.setBounds(0, 0, px, px)
        drawable.draw(c)

        val mask = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        val radius = px * 0.225f
        c.drawRoundRect(RectF(0f, 0f, px.toFloat(), px.toFloat()), radius, radius, mask)

        iconCache[key] = out
        return out
    }
}
