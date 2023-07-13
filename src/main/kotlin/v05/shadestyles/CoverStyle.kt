package v05.shadestyles

import org.openrndr.draw.VertexBuffer
import org.openrndr.draw.shadeStyle

fun coverStyle(instanceAttributes: VertexBuffer) = shadeStyle {
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
                        
                        uv.x += xamp * fcos(va_texCoord0.y * xfreq + p_time);
                        uv.y += yamp * fcos(va_texCoord0.x * yfreq + p_time);
                        
                        uv = (rm2 * (uv-vec2(0.5))) + vec2(0.5);
                        
                        float xp = fcos(vi_umap0.x * 114.0 * uv.x) * 0.5 + 0.5;
                        float yp = fcos(vi_umap0.y * 114.0 * uv.y) * 0.5 + 0.5;

                        vec3 ogfill = x_fill.rgb;                        
                        x_fill.rgb *= (xp*yp);
                        x_fill.rgb = mix(ogfill, x_fill.rgb, p_fade);
                    """.trimIndent()
    attributes(instanceAttributes)
    parameter("time", 0.0)
}