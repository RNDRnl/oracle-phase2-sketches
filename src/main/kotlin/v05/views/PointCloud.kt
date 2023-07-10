package v05.views

import org.openrndr.Clock
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.shadeStyle
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.smoothstep
import org.openrndr.math.transforms.unproject
import org.openrndr.shape.Rectangle
import v05.DataModel
import v05.State
import v05.facultyColor


val pointCloudShadeStyle by lazy {
    shadeStyle {
        fragmentTransform = """float dx = cos(v_worldPosition.x*10.0)*2.0+2.0; 
            float dy = cos(v_worldPosition.y*10.0)*2.0+2.0;
float c = 0.5 + 0.5 * cos(va_texCoord0.y * 3.1415 * dx + dy + p_time) * sin(va_texCoord0.x * 3.1415 * dy + dx + p_time);
x_fill.rgb *= c;                             
        """
    }
}

class PointCloud(val drawer: Drawer, val clock: Clock, val state: State, val data: DataModel) {
    fun draw() {

        val up = unproject(Vector3(drawer.width/2.0, drawer.height/2.0, 1.0),drawer.projection, drawer.view * drawer.model, drawer.width, drawer.height)

        val center = up.xy

        drawer.isolated {
            pointCloudShadeStyle.parameter("time", clock.seconds)
            drawer.shadeStyle = pointCloudShadeStyle
            drawer.rectangles {
                var idx = 0
                for ((point, article) in data.pointsToArticles) {
                    val opacity = if (state.filtered[point] != null) 1.0 else 0.2
                    this.stroke = if (state.activePoints[point] != null) article.faculty.facultyColor().mix(
                        ColorRGBa.WHITE, 0.75
                    ) else null

                    val size = (if (state.filtered[point] != null) 3.0 else 1.0) * 0.75

                    this.fill = article.faculty.facultyColor().opacify(opacity)


                    val r = (data.rotations[idx])

                    this.rectangle(Rectangle.fromCenter(point, 0.707 * size,  size), r)
                    idx++
                }
            }

        }
    }
}