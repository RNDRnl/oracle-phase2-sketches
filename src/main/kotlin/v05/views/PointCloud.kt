package v05.views

import org.openrndr.Clock
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.shadeStyle
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.math.smoothstep
import org.openrndr.math.transforms.buildTransform
import org.openrndr.math.transforms.unproject
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contains
import v05.*


val pointCloudShadeStyle by lazy {
    shadeStyle {
        fragmentTransform = """float dx = cos(v_worldPosition.x*10.0)*2.0+2.0; 
            float dy = cos(v_worldPosition.y*10.0)*2.0+2.0;
float c = 0.5 + 0.5 * cos(va_texCoord0.y * 3.1415 * dx + dy + p_time) * sin(va_texCoord0.x * 3.1415 * dy + dx + p_time);
            vec3 black = vec3(0.2);
            vec3 white = vec3(1.0);
            vec3 color = x_fill.rgb;
            vec3 res = mix(mix(black, color, min(1.0,c*2.0)), white, max(0.0, c*2.0 - 1.0) ) ; 

x_fill.rgb = mix(x_fill.rgb, res, p_fade);                             
        """
    }
}

class PointCloud(val drawer: Drawer, val clock: Clock, val state: State, val data: DataModel) {

    var currentlySelected = Rectangle(0.0, 0.0, 3.0, 0.75) to 0.0
    var closestArticle = state.filtered[state.closest]

    fun draw() {

        val up = unproject(Vector3(drawer.width/2.0, drawer.height/2.0, 1.0),drawer.projection, drawer.view * drawer.model, drawer.width, drawer.height)
        val center = up.xy

        closestArticle = state.filtered[state.closest]

        drawer.isolated {
            pointCloudShadeStyle.parameter("time", clock.seconds)
            val scale = (drawer.view * Vector4(1.0, 1.0, 0.0, 0.0)).xy.length
            pointCloudShadeStyle.parameter("fade", smoothstep(20.0, 30.0, scale))
            drawer.shadeStyle = pointCloudShadeStyle
            drawer.rectangles {
                var idx = 0
                for ((point, article) in data.pointsToArticles) {

                    val size = (if (state.filtered[point] != null) 3.0 else 1.0) * 0.75
                    val rotation = (data.rotations[idx])
                    val rect = Rectangle.fromCenter(point, 0.707 * size,  size)

                    val isInRect = (state.closest == point && point in rect && state.zoom in CLOSEST)

                    val opacity = if (state.filtered[point] != null) 1.0 else 0.2
                    this.stroke = if (
                        (state.activePoints[point] != null && state.zoom in CLOSER) || isInRect) article.faculty.facultyColor().mix(
                        ColorRGBa.WHITE, 0.75
                    ) else null


                    this.fill = article.faculty.facultyColor().opacify(opacity)



                    this.rectangle(rect, rotation)
                    idx++

                    if(article == closestArticle) currentlySelected = rect to rotation

                }
            }


        }




    }
}