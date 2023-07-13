package v05.lab

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.openrndr.application
import org.openrndr.draw.VertexElementType
import org.openrndr.draw.shadeStyle
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import org.openrndr.shape.map
import java.io.File
import kotlin.math.sqrt

fun main() {
    application {
        configure {
            width = 1280
            height = 1024
        }
        oliveProgram {

            val points = csvReader().open(File("offline-data/umap-2d-v5.csv")) {
                readAllWithHeaderAsSequence().map {
                    Vector2(it["x"]!!.toDouble(), it["y"]!!.toDouble())
                }.toList()
            }

            val umap4 = csvReader().open(File("offline-data/umap-8d-v5.csv")) {
                readAllWithHeaderAsSequence().map {
                    Pair(
                    Vector4(it["0"]!!.toDouble(), it["1"]!!.toDouble(),
                        it["2"]!!.toDouble(),it["3"]!!.toDouble()
                        ),
                        Vector4(it["4"]!!.toDouble(), it["5"]!!.toDouble(),
                            it["6"]!!.toDouble(),it["7"]!!.toDouble()
                        ))

                }.toList()
            }

            val pb = points.bounds

            val plot = points.map { it.map(pb, drawer.bounds) }

            val instanceAttributes = vertexBuffer(vertexFormat {
                attribute("umap0", VertexElementType.VECTOR4_FLOAT32)
                attribute("umap1", VertexElementType.VECTOR4_FLOAT32)
            }, points.size)


            instanceAttributes.put {
                for (u in umap4) {
                    write(u.first)
                    write(u.second)
                }
            }

            extend(Camera2D())
            extend {

                drawer.shadeStyle = shadeStyle {

                    fragmentPreamble = """
float fcos(in float x) {
    float w = fwidth(x);
    return cos(x) * smoothstep( 3.1415926535*1.0, 0.0, w );
}                        
                    """.trimIndent()

                    fragmentTransform = """

                        float rot = vi_umap1.z * 3.141592 * 2.0;
                        float c = cos(rot);
                        float s = sin(rot);
                        mat2 rm = mat2(vec2(c, -s), vec2(s, c));
                        
                        
                        float rot2 = vi_umap1.w * 3.141592 * 2.0;
                        float c2 = cos(rot2);
                        float s2 = sin(rot2);
                        mat2 rm2 = mat2(vec2(c2, -s2), vec2(s2, c2));
                        
                        vec2 uv = va_texCoord0;
                        uv = (rm * (uv-vec2(0.5))) + vec2(0.5);
                        
                        
                        float xamp = vi_umap0.z;
                        float xfreq = vi_umap0.w * 3.1415926535 * 4.0;                        
                        
                        float yamp = vi_umap1.x;
                        float yfreq = vi_umap1.y * 3.1415926535 * 4.0;
                        
                        uv.x += xamp * fcos(va_texCoord0.y * xfreq);
                        uv.y += yamp * fcos(va_texCoord0.x * yfreq);
                        
                        uv = (rm2 * (uv-vec2(0.5))) + vec2(0.5);
                        
                        float xp = fcos(vi_umap0.x * 114.0 * uv.x) * 0.5 + 0.5;
                        float yp = fcos(vi_umap0.y * 114.0 * uv.y) * 0.5 + 0.5;
//                        
                        x_fill.r *= vi_umap0.w*0.5 + 0.5;
                        x_fill.g *= vi_umap1.y * 0.5 + 0.5;
                        x_fill.b *= vi_umap1.z * 0.5 + 0.5;
                        //x_fill.a *= vi_umap1.w * 0.5 + 0.5;
                        x_fill.rgb *= (xp*yp);
                    """.trimIndent()
                    attributes(instanceAttributes)
                }

                drawer.stroke = null
                drawer.rectangles {
                    for (p in plot) {
                        rectangle(Rectangle.fromCenter(p, 1.25 * 10.0, 1.25 * 10.0 * sqrt(2.0)))
                    }
                }
            }
        }
    }
}