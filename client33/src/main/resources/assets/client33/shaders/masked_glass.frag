#version 330 core
#extension GL_ARB_separate_shader_objects : require
layout (location = 0) in vec2 uv;
layout (location = 0) out vec4 color;
uniform sampler2D u_Texture;
layout (std140) uniform GlassData {
    vec4 u_Bounds;
    vec4 u_Viewport;
};
void main() {
    vec2 p = vec2(uv.x, 1.0 - uv.y) * u_Viewport.xy;
    vec2 halfSize = max(u_Bounds.zw * 0.5, vec2(0.0));
    float radius = min(u_Viewport.z, min(halfSize.x, halfSize.y));
    vec2 q = abs(p - u_Bounds.xy - halfSize) - halfSize + radius;
    float distanceToEdge = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
    float coverage = 1.0 - smoothstep(-0.75, 0.75, distanceToEdge);
    if (coverage <= 0.0) discard;
    vec3 scene = texture(u_Texture, uv).rgb;
    float rim = exp(-abs(distanceToEdge) * 0.55) * 0.035;
    color = vec4(scene + vec3(0.55, 0.7, 1.0) * rim, coverage * u_Viewport.w);
}
