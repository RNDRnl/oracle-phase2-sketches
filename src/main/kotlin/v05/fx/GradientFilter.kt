package v05.fx

import org.openrndr.draw.Filter1to1
import org.openrndr.draw.filterShaderFromCode

class GradientFilter : Filter1to1(shader = filterShaderFromCode("""uniform sampler2D tex0;
in vec2 v_texCoord0;
out vec4 o_output;
void main() {
    vec2 step = 1.0 / textureSize(tex0, 0);
    float y0 = texture(tex0, v_texCoord0 + step * vec2(0.0, -1.0)).r;
    float y1 = texture(tex0, v_texCoord0 + step * vec2(0.0, 1.0)).r;
    float x0 = texture(tex0, v_texCoord0 + step * vec2(-1.0, 0.0)).r;
    float x1 = texture(tex0, v_texCoord0 + step * vec2(1.0, 0.0)).r;
    
    float dy = y1 - y0;
    float dx = x1 - x0;
    
    vec2 n = normalize(vec2(dx, dy));
    o_output = vec4(n, 0.0, 1.0);
         
}
    
""","gradient"))

