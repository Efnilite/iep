package dev.efnilite.iep.generator

import org.bukkit.util.Vector
import org.jetbrains.annotations.Contract
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

data class Ring(val heading: Vector, val center: Vector, val radius: Int) {

    init {
        assert(radius >= 0)
        assert(heading.isNormalized)
    }

    val blocks = getPositions()

    /**
     * Returns a list of vectors in a circle around [center] with radius [radius].
     * @return A list of vectors in a circle.
     */
    @Contract(pure = true)
    fun getPositions(): List<Vector> {
        if (radius == 0) {
            return emptyList()
        }

        val blocks = mutableListOf<Vector>()
        val centerX = if (heading.x == 0.0) center.x.toInt() else center.z.toInt()
        val centerY = center.y.toInt()

        val accuracy = 30
        var t = 0.0
        repeat(accuracy) {
            t += 2 * Math.PI / accuracy

            val x = (centerX + radius * cos(t)).toInt()
            val y = (centerY + radius * sin(t)).toInt()

            blocks.add(Vector(if (heading.z == 0.0) x else 0, y, if (heading.x == 0.0) x else 0))
        }

        return blocks
    }

    /**
     * Returns whether the given vector is near the ring's center.
     * @param vector The vector to check.
     * @return Whether the given vector is near the ring's center.
     */
    @Contract(pure=true)
    fun isNear(vector: Vector): Boolean {
        val x = if (heading.x == 0.0) Pair(center.x, vector.x) else Pair(center.z, vector.z)
        val dx = abs(x.first - x.second)
        val dy = abs(center.y - vector.y)

        return dy <= radius - 1 && dx <= 2
    }
}