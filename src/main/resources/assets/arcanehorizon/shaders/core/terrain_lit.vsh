#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:sample_lightmap.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in ivec2 UV2;
uniform sampler2D Sampler2;
layout(location = 0) out vec4 vertexColor;

void main() {
    vec4 cameraSpace = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * cameraSpace;
    vertexColor = vec4(Color.rgb * sample_lightmap(Sampler2, UV2).rgb, Color.a);
}
