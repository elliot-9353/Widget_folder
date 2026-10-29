package com.example.folderwidget.shapes

import android.graphics.Path
import android.graphics.PointF
import com.example.folderwidget.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Wavy "flower" blob with one lobe per icon, icons on a ring around the center. */
object FlowerShape : FolderShape {
    override val id = "flower"
    override val nameRes = R.string.shape_flower
    override val minApps = 3
    override val maxApps = 8

    private const val RING = 0.29f   // radius of the icon ring (fraction of size)
    private const val BASE = 0.44f   // average radius of the blob
    private const val AMP = 0.035f   // how deep the lobes are
    private val HALF_PI = (PI / 2).toFloat()

    override fun outline(count: Int, size: Float): Path {
        val c = size / 2f
        val steps = 360
        val path = Path()
        for (i in 0..steps) {
            val a = (2.0 * PI * i / steps).toFloat()
            val r = size * (BASE + AMP * cos(count * a))
            val t = a - HALF_PI // first lobe points up
            val x = c + r * cos(t)
            val y = c + r * sin(t)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    override fun slots(count: Int, size: Float): List<PointF> {
        val c = size / 2f
        return List(count) { i ->
            val a = (2.0 * PI * i / count - PI / 2).toFloat()
            PointF(c + size * RING * cos(a), c + size * RING * sin(a))
        }
    }

    override fun iconSize(count: Int, size: Float): Float {
        val spacing = 2f * size * RING * sin((PI / count).toFloat())
        return min(size * 0.26f, spacing * 0.84f)
    }
}
