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
#define d_primitive d_circle
// </primitive-types>
uniform float p_t; 

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
in vec2 vi_radius;
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
void main(void) {
        // -- fragmentConstants
    int c_instance = v_instance;
    int c_element = 0;
    vec2 c_screenPosition = gl_FragCoord.xy / u_contentScale;
    float c_contourPosition = 0.0;
    vec3 c_boundsPosition = vec3(va_texCoord0, 0.0);
    vec3 c_boundsSize = v_boundsSize;
    float smoothFactor = 3.0;

    vec4 x_fill = vi_fill;
    vec4 x_stroke = vi_stroke;
    float x_strokeWeight = vi_strokeWeight;
    
    {
        float dist = distance(c_boundsPosition.xy, vec2(0.5));
float c = smoothstep(0.4, 0.8, dist);
x_fill = vec4(vec3(c), 1.0 - t);
    }
    float wd = fwidth(length(va_texCoord0 - vec2(0.0)));
    float d = length(va_texCoord0 - vec2(0.5)) * 2;

    float or = smoothstep(0, wd * smoothFactor, 1.0 - d);
    float b = x_strokeWeight / vi_radius.x;
    float ir = smoothstep(0, wd * smoothFactor, 1.0 - b - d);

    vec4 final = vec4(0.0);
    final.rgb =  x_stroke.rgb;
    final.a = or * (1.0 - ir) * x_stroke.a;
    final.rgb *= final.a;

    final.rgb += x_fill.rgb * ir * x_fill.a;
    final.a += ir * x_fill.a;
    o_color = final;
}
// -------------
// shade-style-custom:circle--1167670038
// created 2023-05-19T11:15:53.523002900
/*
0(74) : error C1503: undefined variable "t"
*/
