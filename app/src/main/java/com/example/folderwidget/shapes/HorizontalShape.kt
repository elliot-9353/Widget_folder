package com.example.folderwidget.shapes

import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import com.example.folderwidget.R
import kotlin.math.min

/** Full-width rounded horizontal folder with one evenly spaced row of app icons. */
object HorizontalShape : FolderShape {
    override val id = "horizontal"
    override val nameRes = R.string.shape_horizontal
    override val minApps = 1
    override val maxApps = 7
    override val previewAspectRatio = 4f
    override val showCenterMark = false

    // ---- Tunable look (change only these numbers) ----
    /** Height of the bar as a fraction of the widget height (the rest is empty margin, top and bottom). */
    private const val BAR_HEIGHT = 0.66f
    /** Empty margin at each side as a fraction of the widget width (0 = touch the widget edges). */
    private const val SIDE_MARGIN = 0.006f
    /** Corner radius as a fraction of the bar height (0.5 = full pill, 0 = square corners). */
    private const val CORNER = 0.22f
    /** Max icon size as a fraction of the bar height. */
    private const val ICON_OF_BAR = 0.88f
    /** Max icon size as a fraction of the width available for one icon. */
    private const val ICON_OF_SLOT = 0.90f

    // Legacy square methods are retained for interface compatibility.
    override fun outline(count: Int, size: Float): Path = outline(count, size, size)
    override fun slots(count: Int, size: Float): List<PointF> = slots(count, size, size)
    override fun iconSize(count: Int, size: Float): Float = iconSize(count, size, size)

    private fun bar(width: Float, height: Float): RectF {
        val insetX = width * SIDE_MARGIN
        val insetY = height * (1f - BAR_HEIGHT) / 2f
        return RectF(insetX, insetY, width - insetX, height - insetY)
    }

    override fun outline(count: Int, width: Float, height: Float): Path {
        val rect = bar(width, height)
        val radius = rect.height() * CORNER
        return Path().apply {
            addRoundRect(rect, radius, radius, Path.Direction.CW)
        }
    }

    override fun slots(count: Int, width: Float, height: Float): List<PointF> {
        val safeCount = count.coerceAtLeast(1)
        val rect = bar(width, height)
        val y = height / 2f
        return List(safeCount) { index ->
            PointF(rect.left + rect.width() * ((index + 0.5f) / safeCount), y)
        }
    }

    override fun iconSize(count: Int, width: Float, height: Float): Float {
        val safeCount = count.coerceAtLeast(1)
        val rect = bar(width, height)
        val slotWidth = rect.width() / safeCount
        return min(rect.height() * ICON_OF_BAR, slotWidth * ICON_OF_SLOT)
    }
}
