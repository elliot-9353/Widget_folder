package com.example.folderwidget.shapes

import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import com.example.folderwidget.R

/**
 * Resizable folder. Icons never shrink: a bigger widget gets more cells, a smaller one gets
 * fewer. The folder body hugs the cells (cells + a thin padding) and sits in the middle of the
 * widget, so a one-row or one-column widget gives a thin strip and nothing is wasted.
 * When the apps do not fit, the last cell becomes the "open list" button (four dots).
 */
object GridShape : AdaptiveShape {
    // ---- Tunable look (dp). PITCH_DP and PAD_DP must match res/layout/widget_grid.xml ----
    /** Size of one cell (icon + space around it). */
    const val PITCH_DP = 43f
    /** Size of one app icon. */
    const val ICON_DP = 34f
    /** Padding between the folder edge and the cells. */
    const val PAD_DP = 4f
    const val CORNER_DP = 15f

    /** Largest grid. Must match the number of rows/columns in res/layout/widget_grid.xml. */
    const val MAX_COLS = 8
    const val MAX_ROWS = 10

    // ---- How the cell count is derived from the size the launcher reports ----
    /**
     * The launcher reports a width smaller than what is really drawn (measured on a real device:
     * about 16dp less, i.e. its own padding), so this much is added back before counting columns.
     * Lower it if the folder ever gets wider than the widget, raise it if a column is missing.
     */
    private const val WIDTH_BONUS_DP = 16f
    /** The reported height is a little taller than the drawn one, so it is shrunk before counting rows. */
    private const val HEIGHT_FACTOR = 0.96f
    /** Extra dp allowed when counting (a cell count is accepted if it misses by at most this much). */
    private const val TOLERANCE_DP = 2f

    private const val DEFAULT_PX_PER_DP = 2f

    override val id = "grid"
    override val nameRes = R.string.shape_grid
    override val minApps = 1
    override val maxApps = MAX_COLS * MAX_ROWS
    override val maxSelectable = MAX_COLS * MAX_ROWS
    override val showCenterMark = false

    /** Columns and rows whose folder (cells + padding) fits a widget of [wDp] x [hDp]. */
    fun fit(wDp: Float, hDp: Float): Pair<Int, Int> {
        val cols = ((wDp + WIDTH_BONUS_DP - 2f * PAD_DP + TOLERANCE_DP) / PITCH_DP).toInt().coerceIn(1, MAX_COLS)
        val rows = ((hDp * HEIGHT_FACTOR - 2f * PAD_DP + TOLERANCE_DP) / PITCH_DP).toInt().coerceIn(1, MAX_ROWS)
        return Pair(cols, rows)
    }

    /**
     * Layout inside the lattice itself (origin 0,0, size [latticeW] x [latticeH] pixels).
     * Used by the widget, which shows the lattice unscaled in the middle of its body.
     */
    fun layout(cols: Int, rows: Int, total: Int, latticeW: Float, latticeH: Float, pxPerDp: Float): GridPlan {
        val pitchX = latticeW / cols
        val pitchY = latticeH / rows
        val slots = cols * rows
        val showMore = total > slots
        val shown = if (showMore) slots - 1 else total
        val used = shown + if (showMore) 1 else 0
        val centers = List(used) { i ->
            PointF((i % cols + 0.5f) * pitchX, (i / cols + 0.5f) * pitchY)
        }
        return GridPlan(
            cols = cols,
            rows = rows,
            shown = shown,
            showMore = showMore,
            centers = centers,
            iconSize = ICON_DP * pxPerDp,
            body = bodyPath(RectF(0f, 0f, latticeW, latticeH), pxPerDp),
            originX = 0f,
            originY = 0f,
        )
    }

    /** Whole-widget layout: the folder (cells + padding) centered in a [widthPx] x [heightPx] area. Used for previews. */
    override fun plan(total: Int, widthPx: Float, heightPx: Float, pxPerDp: Float): GridPlan {
        val (cols, rows) = fit(widthPx / pxPerDp, heightPx / pxPerDp)
        val latticeW = cols * PITCH_DP * pxPerDp
        val latticeH = rows * PITCH_DP * pxPerDp
        val pad = PAD_DP * pxPerDp
        val inner = layout(cols, rows, total, latticeW, latticeH, pxPerDp)
        val ox = (widthPx - latticeW) / 2f
        val oy = (heightPx - latticeH) / 2f
        return GridPlan(
            cols = cols,
            rows = rows,
            shown = inner.shown,
            showMore = inner.showMore,
            centers = inner.centers.map { PointF(it.x + ox, it.y + oy) },
            iconSize = inner.iconSize,
            body = bodyPath(RectF(ox - pad, oy - pad, ox + latticeW + pad, oy + latticeH + pad), pxPerDp),
            originX = ox,
            originY = oy,
        )
    }

    private fun bodyPath(rect: RectF, pxPerDp: Float): Path {
        val radius = (CORNER_DP * pxPerDp).coerceAtMost(minOf(rect.width(), rect.height()) / 2f)
        return Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) }
    }

    // Legacy methods required by FolderShape (the renderer uses plan() for this shape).
    override fun outline(count: Int, size: Float): Path = plan(count, size, size, DEFAULT_PX_PER_DP).body
    override fun slots(count: Int, size: Float): List<PointF> = plan(count, size, size, DEFAULT_PX_PER_DP).centers
    override fun iconSize(count: Int, size: Float): Float = ICON_DP * DEFAULT_PX_PER_DP
    override fun outline(count: Int, width: Float, height: Float): Path = plan(count, width, height, DEFAULT_PX_PER_DP).body
    override fun slots(count: Int, width: Float, height: Float): List<PointF> =
        plan(count, width, height, DEFAULT_PX_PER_DP).centers
    override fun iconSize(count: Int, width: Float, height: Float): Float = ICON_DP * DEFAULT_PX_PER_DP
}
