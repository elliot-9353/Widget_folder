package com.example.folderwidget.render

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.core.graphics.ColorUtils
import com.example.folderwidget.data.FolderConfig
import com.example.folderwidget.shapes.AdaptiveShape
import com.example.folderwidget.shapes.FolderShapes
import com.example.folderwidget.shapes.GridPlan
import com.example.folderwidget.shapes.GridShape
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Draws a folder shape and its app icons into a bitmap matching the widget's aspect ratio. */
object FolderRenderer {
    private val iconCache = HashMap<String, Bitmap>()

    /** Where the icons are, in bitmap pixels. Used by the tap-target grid of the classic shapes. */
    class Layout(
        val pkgs: List<String>,
        val centers: List<PointF>,
        val iconSize: Float,
        val cx: Float,
        val cy: Float,
        val size: Float,
        val showCenterMark: Boolean,
    )

    /** Apps drawn on the widget itself. The rest (if any) only appear in the popup list. */
    fun visibleApps(cfg: FolderConfig): List<String> {
        val shape = FolderShapes.byId(cfg.shapeId)
        return cfg.apps
            .take(shape.maxSelectable)
            .take(shape.maxApps)
    }

    fun layoutOf(cfg: FolderConfig, widthPx: Int, heightPx: Int): Layout {
        val width = widthPx.toFloat()
        val height = heightPx.toFloat()
        val shape = FolderShapes.byId(cfg.shapeId)
        val pkgs = visibleApps(cfg)
        val slotCount = pkgs.size.coerceAtLeast(shape.minApps)
        val centers = shape.slots(slotCount, width, height).take(pkgs.size)
        return Layout(
            pkgs = pkgs,
            centers = centers,
            iconSize = shape.iconSize(slotCount, width, height),
            cx = width / 2f,
            cy = height / 2f,
            size = min(width, height),
            showCenterMark = shape.showCenterMark,
        )
    }

    /** Icons only (no folder body) in a bitmap exactly as big as the cell lattice. Used by the grid widget. */
    fun renderGridIcons(ctx: Context, cfg: FolderConfig, plan: GridPlan, widthPx: Int, heightPx: Int): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        drawPlan(ctx, Canvas(bmp), cfg, visibleApps(cfg), plan)
        return bmp
    }

    fun render(
        ctx: Context,
        cfg: FolderConfig,
        widthPx: Int,
        heightPx: Int = widthPx,
        pxPerDp: Float = 2f,
    ): Bitmap {
        val width = widthPx.toFloat()
        val height = heightPx.toFloat()
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val shape = FolderShapes.byId(cfg.shapeId)
        val pkgs = visibleApps(cfg)
        val plan = (shape as? AdaptiveShape)?.plan(pkgs.size, width, height, pxPerDp)
        val slotCount = pkgs.size.coerceAtLeast(shape.minApps)

        // Body: each shape receives the actual widget width and height.
        val outline = plan?.body
            ?: shape.outline(slotCount, width, height, cfg.roundness.coerceIn(0, 100) / 100f)
        // Transparency only affects the folder body; the app icons stay fully opaque.
        val bodyAlpha = 255 * (100 - cfg.transparency.coerceIn(0, 100)) / 100
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = ColorUtils.setAlphaComponent(cfg.color, bodyAlpha)
        }
        canvas.drawPath(outline, fill)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = min(width, height) * 0.012f
            color = ColorUtils.setAlphaComponent(
                ColorUtils.blendARGB(cfg.color, Color.BLACK, 0.3f),
                bodyAlpha,
            )
        }
        if (plan == null) canvas.drawPath(outline, stroke)

        if (plan != null) {
            drawPlan(ctx, canvas, cfg, pkgs, plan)
            return bmp
        }

        // Icons use the exact same slot geometry as the tap-target calculation.
        val iconSize = shape.iconSize(slotCount, width, height)
        val slots = shape.slots(slotCount, width, height)
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        val half = iconSize / 2f
        pkgs.forEachIndexed { i, pkg ->
            val center = slots.getOrNull(i) ?: return@forEachIndexed
            val appIcon = icon(ctx, pkg, iconSize.roundToInt()) ?: return@forEachIndexed
            canvas.drawBitmap(
                appIcon,
                null,
                RectF(center.x - half, center.y - half, center.x + half, center.y + half),
                iconPaint,
            )
        }

        // Center mark is retained for the original flower shape only.
        if (shape.showCenterMark) {
            val light = ColorUtils.calculateLuminance(cfg.color) > 0.5
            val dots = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (light) 0x73000000 else 0xB3FFFFFF.toInt()
            }
            val cx = width / 2f
            val cy = height / 2f
            val unit = min(width, height)
            val off = unit * 0.032f
            val radius = unit * 0.014f
            for (dx in listOf(-off, off)) for (dy in listOf(-off, off)) {
                canvas.drawCircle(cx + dx, cy + dy, radius, dots)
            }
        }
        return bmp
    }

    /** Icons of an adaptive shape, plus the "open list" dots button when the apps do not all fit. */
    private fun drawPlan(ctx: Context, canvas: Canvas, cfg: FolderConfig, pkgs: List<String>, plan: GridPlan) {
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        val size = plan.iconSize
        val half = size / 2f
        for (i in 0 until plan.shown) {
            val c = plan.centers[i]
            val appIcon = icon(ctx, pkgs[i], size.roundToInt(), fill = true) ?: continue
            canvas.drawBitmap(appIcon, null, RectF(c.x - half, c.y - half, c.x + half, c.y + half), iconPaint)
        }

        if (plan.showMore) {
            // Same four-dot mark as the center of the flower shape.
            val c = plan.centers[plan.shown]
            val light = ColorUtils.calculateLuminance(cfg.color) > 0.5 && cfg.transparency < 60
            val dots = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (light) 0x99000000.toInt() else 0xE6FFFFFF.toInt()
            }
            val off = size * 0.16f
            val radius = size * 0.075f
            for (dx in listOf(-off, off)) for (dy in listOf(-off, off)) {
                canvas.drawCircle(c.x + dx, c.y + dy, radius, dots)
            }
        }
    }

    /** App icon clipped to an anti-aliased rounded square. Cached per package and size. */
    private fun icon(ctx: Context, pkg: String, px: Int, fill: Boolean = false): Bitmap? {
        if (px <= 0) return null
        val key = "$pkg@$px@$fill"
        iconCache[key]?.let { return it }

        val drawable = try {
            ctx.packageManager.getApplicationIcon(pkg)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }

        val out = if (fill) {
            filledIcon(drawable, px)
        } else {
            Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888).also {
                drawable.setBounds(0, 0, px, px)
                drawable.draw(Canvas(it))
            }
        }
        val c = Canvas(out)

        val mask = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        val radius = px * 0.225f
        c.drawRoundRect(RectF(0f, 0f, px.toFloat(), px.toFloat()), radius, radius, mask)

        iconCache[key] = out
        return out
    }

    /**
     * An icon that fills its whole box. Adaptive icons are drawn with their 25% bleed outside the
     * box (as launchers do), and any transparent margin left around the art is cropped away, so
     * every icon ends up as large as the others.
     */
    private fun filledIcon(drawable: Drawable, px: Int): Bitmap {
        val big = (px * 1.6f).roundToInt().coerceAtLeast(px)
        val src = Bitmap.createBitmap(big, big, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(src)
        if (drawable is AdaptiveIconDrawable) {
            val extra = (big * 0.25f).roundToInt()
            drawable.background?.let {
                it.setBounds(-extra, -extra, big + extra, big + extra)
                it.draw(canvas)
            }
            drawable.foreground?.let {
                it.setBounds(-extra, -extra, big + extra, big + extra)
                it.draw(canvas)
            }
        } else {
            drawable.setBounds(0, 0, big, big)
            drawable.draw(canvas)
        }

        val pixels = IntArray(big * big)
        src.getPixels(pixels, 0, big, 0, 0, big, big)
        var minX = big
        var minY = big
        var maxX = -1
        var maxY = -1
        for (y in 0 until big) for (x in 0 until big) {
            if ((pixels[y * big + x] ushr 24) > 40) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }

        val out = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        if (maxX < 0) {
            Canvas(out).drawBitmap(src, null, Rect(0, 0, px, px), paint)
            return out
        }
        val side = max(maxX - minX + 1, maxY - minY + 1).coerceIn(1, big)
        val left = (((minX + maxX) / 2f) - side / 2f).roundToInt().coerceIn(0, big - side)
        val top = (((minY + maxY) / 2f) - side / 2f).roundToInt().coerceIn(0, big - side)
        Canvas(out).drawBitmap(src, Rect(left, top, left + side, top + side), Rect(0, 0, px, px), paint)
        return out
    }
}
