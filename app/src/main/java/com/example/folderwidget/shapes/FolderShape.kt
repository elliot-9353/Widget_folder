package com.example.folderwidget.shapes

import android.graphics.Path
import android.graphics.PointF
import androidx.annotation.StringRes
import kotlin.math.min

/**
 * A folder shape. Legacy square methods remain the base contract so existing shapes
 * keep their exact geometry. Rectangular overloads let new shapes use the full widget.
 */
interface FolderShape {
    /** Stable id saved in preferences. Never change it after release. */
    val id: String

    /** Display name shown in the settings screen. */
    @get:StringRes
    val nameRes: Int

    val minApps: Int

    /** Max apps drawn on the widget itself. */
    val maxApps: Int

    /** Max apps the user can pick. Apps beyond [maxApps] only show in the popup list. */
    val maxSelectable: Int get() = maxApps

    /** Aspect ratio used by the settings preview (width / height). */
    val previewAspectRatio: Float get() = 1f

    /** True if the outline can morph from its normal look (0) into a circle (1). */
    val supportsRoundness: Boolean get() = false

    /** Draw the dots in the middle that hint "tap to open". */
    val showCenterMark: Boolean get() = true

    /** Legacy square geometry. */
    fun outline(count: Int, size: Float): Path
    fun slots(count: Int, size: Float): List<PointF>
    fun iconSize(count: Int, size: Float): Float

    /** Rectangular geometry; by default, preserve the old centered square behavior. */
    fun outline(count: Int, width: Float, height: Float): Path {
        val size = min(width, height)
        return outline(count, size).apply {
            offset((width - size) / 2f, (height - size) / 2f)
        }
    }

    /** Outline with a [roundness] from 0 (normal) to 1 (circle). Shapes that do not support it ignore it. */
    fun outline(count: Int, width: Float, height: Float, roundness: Float): Path = outline(count, width, height)

    fun slots(count: Int, width: Float, height: Float): List<PointF> {
        val size = min(width, height)
        val dx = (width - size) / 2f
        val dy = (height - size) / 2f
        return slots(count, size).map { PointF(it.x + dx, it.y + dy) }
    }

    fun iconSize(count: Int, width: Float, height: Float): Float =
        iconSize(count, min(width, height))
}
