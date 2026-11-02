package com.example.folderwidget.shapes

import android.graphics.Path
import android.graphics.PointF

/** Result of laying out an adaptive shape for one widget size. All coordinates are in bitmap pixels. */
class GridPlan(
    val cols: Int,
    val rows: Int,
    /** Number of apps drawn, taken from the start of the list. */
    val shown: Int,
    /** True when the last cell is a "more" button that opens the full list. */
    val showMore: Boolean,
    /** Center of every used cell: the shown apps first, then the "more" button if any. */
    val centers: List<PointF>,
    val iconSize: Float,
    val body: Path,
    /** Top-left corner of the lattice (cells start here); can be slightly negative. */
    val originX: Float,
    val originY: Float,
)

/**
 * A shape whose icons keep a fixed size (in dp). The number of cells depends on the widget size,
 * and when the apps do not fit, the last cell becomes a "more" button.
 */
interface AdaptiveShape : FolderShape {
    fun plan(total: Int, widthPx: Float, heightPx: Float, pxPerDp: Float): GridPlan
}
