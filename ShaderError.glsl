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
#define d_primitive d_font_image_map
// </primitive-types>

// <drawer-uniforms(true, true)> (ShadeStyleGLSL.kt)
            
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
            
layout(shared) uniform StyleBlock {
    uniform vec4 u_fill;
    uniform vec4 u_stroke;
    uniform float u_strokeWeight;
    uniform float[25] u_colorMatrix;
};
// </drawer-uniforms>

in vec2 a_texCoord0;
in vec4 a_bounds;
in vec3 a_position;
in float a_instance;


out vec2 va_texCoord0;
out vec4 va_bounds;
out vec3 va_position;
out float va_instance;

// <transform-varying-out> (ShadeStyleGLSL.kt)
out vec3 v_worldNormal;
out vec3 v_viewNormal;
out vec3 v_worldPosition;
out vec3 v_viewPosition;
out vec4 v_clipPosition;

flat out mat4 v_modelNormalMatrix;
// </transform-varying-out>

flat out int v_instance;
flat out int v_element;

void main() {
        int c_instance = int(a_position.z);
    int c_element = 0;
    vec3 decodedPosition = vec3(a_position.xy, 0.0);
    v_element = int(a_position.z);
    v_instance = int(a_instance);

        va_texCoord0 = a_texCoord0;
    va_bounds = a_bounds;
    va_position = a_position;
    va_instance = a_instance;

    // <pre-Transform> (ShadeStyleGLSL.kt)
mat4 x_modelMatrix = u_modelMatrix;
mat4 x_viewMatrix = u_viewMatrix;
mat4 x_modelNormalMatrix = u_modelNormalMatrix;
mat4 x_viewNormalMatrix = u_viewNormalMatrix;
mat4 x_projectionMatrix = u_projectionMatrix;
// </pre-transform>
    vec3 x_normal = vec3(0.0, 0.0, 1.0);
    vec3 x_position = decodedPosition;
    {
        vec3 voffset = (x_viewMatrix * vec4(i_offset, 1.0)).xyz;
x_viewMatrix = mat4(1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0);
float size = 0.05;
x_position.xyz *= vec3(size - 0.01, size + 0.015, size);
x_position.xyz += voffset;
    }
    // <post-transform> (ShadeStyleGLSL.kt)
v_worldNormal = (x_modelNormalMatrix * vec4(x_normal,0.0)).xyz;
v_viewNormal = (x_viewNormalMatrix * vec4(v_worldNormal,0.0)).xyz;
v_worldPosition = (x_modelMatrix * vec4(x_position, 1.0)).xyz;
v_viewPosition = (x_viewMatrix * vec4(v_worldPosition, 1.0)).xyz;
v_clipPosition = x_projectionMatrix * vec4(v_viewPosition, 1.0);
v_modelNormalMatrix = x_modelNormalMatrix;
// </post-transform>
    gl_Position = v_clipPosition;
}
            // -------------
// shade-style-custom:font-image-map--400455073
// created 2023-05-05T11:29:51.210155400
/*
0(83) : error C1503: undefined variable "i_offset"
*/
