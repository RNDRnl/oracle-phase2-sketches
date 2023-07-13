package v05.views

import org.openrndr.Clock
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.PropertyAnimationKey
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import org.openrndr.math.mixAngle
import org.openrndr.math.smoothstep
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contains
import v05.*
import v05.shadestyles.coverStyle
import kotlin.math.abs
import kotlin.math.min




class PointCloud(val drawer: Drawer, val clock: Clock, val state: State, val data: DataModel): Animatable() {

    var fader = 1.0
    var currentlySelected: Int? = null
        set(value) {
            if(field != value) {
                fader = 0.0
                if(value == null) {
                    fadeOut().completed.listen {
                        field = null
                    }
                } else {
                    field = value
                    fadeIn()
                }
            }
        }


    private fun rectContour(rect: Rectangle, idx: Int): ShapeContour {
        val rotation = data.rotations[idx]
        println(rotation)
        return rect.contour.transform(buildTransform {
            translate(rect.center)
            rotate(rotation)
            translate(-rect.center)
        })
    }
    fun rect(pos: Vector2, scale: Double = 0.75): Rectangle {
        val size = (if (state.filtered[pos] != null) 3.0 else 1.0) * scale
        return Rectangle.fromCenter(pos, 0.707 * size,  size)
    }

    fun contains(pos: Vector2): Int? {
        val closestPos = state.kdtree.findNearest(pos)
        return if(closestPos != null) {
            val rect = rect(closestPos)
            val idx = data.pointsToArticles.keys.indexOf(closestPos)
            val contour = rectContour(rect, idx)
            if(contour.contains(pos)) {
                idx
            } else {
                null
            }
        } else {
            null
        }

    }

    fun positionChanged() {
        currentlySelected = contains(state.lookAt)
    }

    fun fadeIn() {
        cancel()
        ::fader.animate(1.0, 750, Easing.CubicInOut)
    }

    fun fadeOut(): PropertyAnimationKey<Double> {
        println("out")
        cancel()
        return ::fader.animate(0.0, 350, Easing.CubicInOut)
    }

    var zoom = state.zoom
        set(value) {
            if(field in CLOSEST && value in CLOSER) {
                fadeOut()
            } else if(field in CLOSER && value in CLOSEST) {
                fadeIn()
            }
            field = value
        }
    val instanceAttributes = vertexBuffer(vertexFormat {
        attribute("umap0", VertexElementType.VECTOR4_FLOAT32)
        attribute("umap1", VertexElementType.VECTOR4_FLOAT32)
    }, data.umap8.size)

    init {
        instanceAttributes.put {
            for (u in data.umap8) {
                write(u.first)
                write(u.second)
            }
        }
    }

    val pointCloudShadeStyle = coverStyle(instanceAttributes)

    val fm = loadFont("data/fonts/Roboto-Regular.ttf", 24.0)
    fun draw() {
        updateAnimation()

        zoom = state.zoom

        //val up = unproject(Vector3(drawer.width/2.0, drawer.height/2.0, 1.0),drawer.projection, drawer.view * drawer.model, drawer.width, drawer.height)
        //val center = up.xy

        drawer.isolated {

            pointCloudShadeStyle.parameter("time", clock.seconds*0.1)
            val scale = (drawer.view * Vector4(1.0, 1.0, 0.0, 0.0)).xy.length
            pointCloudShadeStyle.parameter("fade", smoothstep(20.0, 30.0, scale))
            drawer.shadeStyle = pointCloudShadeStyle
            drawer.rectangles {
                var idx = 0
                for ((point, article) in data.pointsToArticles) {

                    val isSelected = (idx == currentlySelected && state.zoom in CLOSEST)



                    // TODO minimum rotation
                    val rotation = if(isSelected) mixAngle(data.rotations[idx], 0.0, fader)
                    else
                        data.rotations[idx]


                    val sc = if (isSelected) 0.75 + (1.1 * fader) else 0.75
                    val rect = rect(point, sc)

                    val opacity = if (state.filtered[point] != null) 1.0 else 0.2
                    this.strokeWeight = 0.2 * fader
                    this.stroke = if ((state.activePoints[point] != null && state.zoom in CLOSER) || isSelected) article.faculty.facultyColor().mix(
                        ColorRGBa.WHITE, 0.75
                    ) else null


                    this.fill = article.faculty.facultyColor().opacify(opacity)
                    this.rectangle(rect, rotation)

                    idx++

                }
            }
        }
    }
}