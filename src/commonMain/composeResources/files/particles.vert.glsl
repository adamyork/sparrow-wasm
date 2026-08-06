#version 310 es
layout(location = 0) in vec2 aUnused;

layout(std430, binding = 0) readonly buffer ParticleBuffer {
    vec4 particleData[];
};

uniform vec2 uViewPort;
uniform vec2 uSurfaceSize;
uniform float uSizeScale;

out vec4 vColor;
out vec2 vLocal;
out float vShape;
out float vFrame;
out float vLifetime;
out float vKind;

const float KIND_DUST = 1.0;
const float KIND_MAP_ITEM_RETURN = 3.0;

const vec2 QUAD[6] = vec2[6](
    vec2(-0.5, -0.5),
    vec2( 0.5, -0.5),
    vec2( 0.5,  0.5),
    vec2(-0.5, -0.5),
    vec2( 0.5,  0.5),
    vec2(-0.5,  0.5)
);

void main() {
    int slot = gl_InstanceID;
    int base = slot * 4;
    vec4 p0 = particleData[base + 0];
    vec4 p1 = particleData[base + 1];
    vec4 p2 = particleData[base + 2];
    vec4 p3 = particleData[base + 3];

    if (p1.w <= 0.5) {
        gl_Position = vec4(-2.0, -2.0, 0.0, 1.0);
        vColor = vec4(0.0);
        vLocal = vec2(0.0);
        vShape = 0.0;
        vFrame = 0.0;
        vLifetime = 1.0;
        vKind = 0.0;
        return;
    }

    float kind = p3.x;
    bool usesUnscaledSize = (kind == KIND_DUST) || (kind == KIND_MAP_ITEM_RETURN);
    float sizeScale = usesUnscaledSize ? 1.0 : uSizeScale;
    float baseSize = max(p1.z, 1.0) * sizeScale;
    vec2 local = QUAD[gl_VertexID] * baseSize;
    vec2 world = p0.xy + local;
    vec2 viewportLocal = world - uViewPort;
    vec2 ndc = vec2(
        (viewportLocal.x / max(uSurfaceSize.x, 1.0)) * 2.0 - 1.0,
        1.0 - (viewportLocal.y / max(uSurfaceSize.y, 1.0)) * 2.0
    );

    gl_Position = vec4(ndc, 0.0, 1.0);
    vColor = p2;
    vLocal = QUAD[gl_VertexID];
    vShape = p3.y;
    vFrame = p1.x;
    vLifetime = max(p1.y, 1.0);
    vKind = kind;
}

