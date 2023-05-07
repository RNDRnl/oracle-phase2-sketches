package orbox

import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Transform
import org.jbox2d.common.Vec2
import org.openrndr.math.Matrix44
import org.openrndr.shape.ShapeContour

fun ShapeContour.polygonShape(scale:Double = 100.0): PolygonShape {
    val linear = sampleLinear()
    val vertices = linear.segments.map { it.start }.map { Vec2((it.x/scale).toFloat(), (it.y/scale).toFloat()) }.toTypedArray()

    return PolygonShape().apply {
        set(vertices, vertices.size)
    }
}

fun Transform.matrix44() : Matrix44 {
    val x = Vec2()
    val y = Vec2()
    this.q.getXAxis(x)
    this.q.getYAxis(y)

    return Matrix44(
        x.x*1.0, y.x * 1.0, 0.0,p.x * 100.0 ,
        x.y * 1.0, y.y * 1.0, 0.0, p.y * 100.0,
        0.0, 0.0, 1.0, 0.0,
        0.0, 0.0, 0.0, 1.0)
}