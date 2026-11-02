#version 330 core
#extension GL_ARB_separate_shader_objects : require

layout (location = 0) in vec2 v_TexCoord;
layout (location = 1) in vec2 v_OneTexel;

uniform sampler2D u_Texture;
uniform sampler2D u_TextureI;

layout (std140) uniform ImageData {
    vec4 u_Color;
    vec4 u_Image; // tiling, image aspect, viewport aspect, reserved
};

layout (location = 0) out vec4 color;

void main() {
    if (texture(u_Texture, v_TexCoord).a == 0.0) discard;
    vec2 imageUv=v_TexCoord-0.5;
    float aspect=u_Image.z/max(u_Image.y,0.001);
    if(aspect>1.0)imageUv.y/=aspect;else imageUv.x*=aspect;
    color = texture(u_TextureI, imageUv*u_Image.x+0.5) * u_Color;
}
