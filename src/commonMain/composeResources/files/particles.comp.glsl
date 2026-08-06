#version 310 es
layout(local_size_x = 64, local_size_y = 1, local_size_z = 1) in;

layout(std430, binding = 0) readonly buffer SrcBuffer {
    vec4 srcData[];
};
layout(std430, binding = 1) writeonly buffer DstBuffer {
    vec4 dstData[];
};
layout(std430, binding = 2) readonly buffer SpawnBuffer {
    vec4 spawnData[];
};

uniform float uDeltaTime;
uniform float uGravity;
uniform float uProjectileSpeed;
uniform float uMapItemReturnSpeed;
uniform float uMapItemReturnMinTravelDist;
uniform vec4 uPlayerRect;

const float KIND_PROJECTILE = 2.0;
const float KIND_MAP_ITEM_RETURN = 3.0;
const float KIND_DUST = 1.0;

void writeParticle(int base, vec4 p0, vec4 p1, vec4 p2, vec4 p3) {
    dstData[base + 0] = p0;
    dstData[base + 1] = p1;
    dstData[base + 2] = p2;
    dstData[base + 3] = p3;
}

void main() {
    uint particleIndex = gl_GlobalInvocationID.x;
    int base = int(particleIndex) * 4;

    vec4 spawn0 = spawnData[base + 0];
    vec4 spawn1 = spawnData[base + 1];
    vec4 spawn2 = spawnData[base + 2];
    vec4 spawn3 = spawnData[base + 3];

    if (spawn1.w > 0.5) {
        writeParticle(base, spawn0, spawn1, spawn2, spawn3);
        return;
    }

    vec4 p0 = srcData[base + 0];
    vec4 p1 = srcData[base + 1];
    vec4 p2 = srcData[base + 2];
    vec4 p3 = srcData[base + 3];

    float alive = p1.w;
    if (alive <= 0.5) {
        writeParticle(base, vec4(0.0), vec4(0.0), vec4(0.0), vec4(0.0));
        return;
    }

    float dtScale = max(uDeltaTime, 0.0001) * 60.0;
    float kind = p3.x;

    if (kind == KIND_PROJECTILE) {
        p1.x += 1.0;
        if (p1.x >= p1.y) {
            writeParticle(base, vec4(0.0), vec4(0.0), vec4(0.0), vec4(0.0));
            return;
        }
        vec2 direction = normalize(vec2(p0.z, p0.w));
        p0.xy += direction * uProjectileSpeed * dtScale;
    } else if (kind == KIND_MAP_ITEM_RETURN) {
        p1.x += 1.0;
        if (p1.x >= p1.y) {
            writeParticle(base, vec4(0.0), vec4(0.0), vec4(0.0), vec4(0.0));
            return;
        }
        vec2 target = vec2(p0.z, p0.w);
        vec2 toTarget = target - p0.xy;
        float dist = length(toTarget);
        if (dist > uMapItemReturnMinTravelDist) {
            vec2 direction = toTarget / max(dist, 0.0001);
            p0.xy += direction * uMapItemReturnSpeed * dtScale;
        }
    } else if (kind == KIND_DUST) {
        p1.x += 1.0;
        if (p1.x >= p1.y) {
            writeParticle(base, vec4(0.0), vec4(0.0), vec4(0.0), vec4(0.0));
            return;
        }
        // Keep dust anchored and make it bloom slightly so it remains visible.
        p1.z = min(p1.z + (0.5 * dtScale), 40.0);
    } else {
        p1.x += 1.0;
        if (p1.x >= p1.y) {
            writeParticle(base, vec4(0.0), vec4(0.0), vec4(0.0), vec4(0.0));
            return;
        }
        p0.w += uGravity * dtScale;
        p0.xy += p0.zw * dtScale;
    }

    writeParticle(base, p0, p1, p2, p3);
}

