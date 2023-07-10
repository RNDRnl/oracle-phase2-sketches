#version 450 core
// <primitive-types> (ShadeStyleGLSL.kt)
#define d_vertex_buffer 0
#define d_image 1
#define d_circle 2
#define d_rectangle 3
#define d_font_image_map 4
#define d_expansion 5
#define d_fast_line 6
#define d_mesh_line 7
#define d_point 8
#define d_custom 9
#define d_primitive d_rectangle
// </primitive-types>



layout(origin_upper_left) in vec4 gl_FragCoord;

// <drawer-uniforms(true, false)> (ShadeStyleGLSL.kt)
            
layout(shared) uniform ContextBlock {
    uniform mat4 u_modelNormalMatrix;
    uniform mat4 u_modelMatrix;
    uniform mat4 u_viewNormalMatrix;
    uniform mat4 u_viewMatrix;
    uniform mat4 u_projectionMatrix;
    uniform float u_contentScale;
    uniform float u_modelViewScalingFactor;
    uniform vec2 u_viewDimensions;
};
            
// </drawer-uniforms>
in vec3 va_position;
in vec3 va_normal;
in vec2 va_texCoord0;
in vec3 vi_offset;
in vec2 vi_dimensions;
in float vi_rotation;
in vec4 vi_fill;
in vec4 vi_stroke;
in float vi_strokeWeight;


// <transform-varying-in> (ShadeStyleGLSL.kt)
in vec3 v_worldNormal;
in vec3 v_viewNormal;
in vec3 v_worldPosition;
in vec3 v_viewPosition;
in vec4 v_clipPosition;
flat in mat4 v_modelNormalMatrix;
// </transform-varying-in>

out vec4 o_color;


flat in int v_instance;
in vec3 v_boundsSize;

    // -- fragmentConstants
    int c_instance = v_instance;
    int c_element = 0;
    vec2 c_screenPosition = gl_FragCoord.xy / u_contentScale;
    float c_contourPosition = 0.0;
    vec3 c_boundsPosition = vec3(va_texCoord0, 0.0);
    vec3 c_boundsSize = v_boundsSize;
    
void main(void) {
    vec4 x_fill = vi_fill;
    vec4 x_stroke = vi_stroke;
    {
        float d = length(2.0 * (va_texCoord0.xy - vec2(0.5));
                float ed = exp(-d); 
    }
    vec2 wd = fwidth(va_texCoord0 - vec2(0.5));
    vec2 d = abs((va_texCoord0 - vec2(0.5)) * 2);

    float irx = smoothstep(0.0, wd.x * 2.5, 1.0-d.x - vi_strokeWeight*2.0/vi_dimensions.x);
    float iry = smoothstep(0.0, wd.y * 2.5, 1.0-d.y - vi_strokeWeight*2.0/vi_dimensions.y);
    float ir = irx*iry;

    vec4 final = vec4(1.0);
    final.rgb = x_fill.rgb * x_fill.a;
    final.a = x_fill.a;

    float sa = (1.0-ir) * x_stroke.a;
    final.rgb = final.rgb * (1.0-sa) + x_stroke.rgb * sa;
    final.a = final.a * (1.0-sa) + sa;

       o_color = final;
}
// -------------
// shade-style-custom:rectangle-1468261408
// created 2023-07-10T11:58:32.635784120
/*
0(72) : error C0000: syntax error, unexpected ';', expecting ')' at token ";"
0(73) : error C1503: undefined variable "d"
*/
