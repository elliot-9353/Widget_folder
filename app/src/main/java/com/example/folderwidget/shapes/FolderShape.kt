package com.example.folderwidget.shapes

import android.graphics.Path
import android.graphics.PointF
import androidx.annotation.StringRes

/**
 * A folder shape. To add a new one:
 *  1. Create a new object implementing this interface (see FlowerShape.kt).
 *  2. Add it to the list in FolderShapes.kt.
 * Nothing else needs to change: the settings screen and the renderer pick it up automatically.
 *
 * All geometry is expressed in a square canvas whose side is [size] pixels.
 */
interface FolderShape {
    /** Stable id saved in preferences. Never change it after release. */
    val id: String

    /** Display name shown in the settings screen. */
    @get:StringRes
    val nameRes: Int

    val minApps: Int
    val maxApps: Int

    /** Draw the dots in the middle that hint "tap to open". */
    val showCenterMark: Boolean get() = true

    /** Outline of the folder for [count] icons. */
    fun outline(count: Int, size: Float): Path

    /** Center point of every icon, in order, for [count] icons. */
    fun slots(count: Int, size: Float): List<PointF>

    /** Side length of each (square) icon for [count] icons. */
    fun iconSize(count: Int, size: Float): Float
}
