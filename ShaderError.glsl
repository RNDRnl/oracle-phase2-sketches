#version 450 core
#define OR_IN_OUT
#define OR_GL
uniform sampler2D tex0;
in vec2 v_texCoord0;
out vec4 o_output;
void main() {
    vec2 step = 1.0 / textureSize(tex0, 0);
    
    float maxR = texture(tex0, v_texCoord0).r;
    bool isMax = true; 
    for (int j = -1; j <= 1; ++j) {
        for (int i = -1; i <= 1; ++i) {
            if (i == 0 && j ==0) {
                continue;                    
            }
            float r = texture(tex0, v_texCoord0 + step * vec2(i, j)).r;
            if (r >= maxR) {
                isxMax = false;                 
            }
        }
    }
    float f = isMax? 1.0: 0.0;
    o_output = vec4(f,f,f, 1.0);
         
}
    
// -------------
// local-maxima
// created 2023-07-10T14:12:11.113212050
/*
0(19) : error C1503: undefined variable "isxMax"
*/
