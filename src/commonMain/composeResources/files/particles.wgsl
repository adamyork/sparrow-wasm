struct ParticleBuffer {
  data: array<vec4<f32>>,
};

struct CollisionSignalBuffer {
  projectileHitCount: atomic<u32>,
  projectileHitXBits: atomic<u32>,
  projectileHitYBits: atomic<u32>,
  projectileHitSizeBits: atomic<u32>,
};

struct ComputeUniforms {
  deltaTime: f32,
  gravity: f32,
  spawnCount: f32,
  maxParticles: f32,
  tickRate: f32,
  simulationSpeed: f32,
  gravityBoost: f32,
  lifetimeDecay: f32,
  dustGrowthPerTick: f32,
  projectileSpeed: f32,
  mapItemReturnSpeed: f32,
  mapItemReturnMinTravelDist: f32,
  playerX: f32,
  playerY: f32,
  playerWidth: f32,
  playerHeight: f32,
};

@group(0) @binding(0) var<storage, read> computeSrcParticles: ParticleBuffer;
@group(0) @binding(1) var<storage, read_write> computeDstParticles: ParticleBuffer;
@group(0) @binding(2) var<storage, read> computeSpawnParticles: ParticleBuffer;
@group(0) @binding(3) var<uniform> computeUniforms: ComputeUniforms;
@group(0) @binding(4) var<storage, read_write> collisionSignal: CollisionSignalBuffer;

@compute @workgroup_size(64)
fn computeMain(@builtin(global_invocation_id) gid: vec3<u32>) {
  let index = gid.x;
  let maxParticles = u32(computeUniforms.maxParticles);
  if (index >= maxParticles) {
    return;
  }

  let base = index * 4u;

  var p0 = computeSrcParticles.data[base + 0u];
  var p1 = computeSrcParticles.data[base + 1u];
  var p2 = computeSrcParticles.data[base + 2u];
  var p3 = computeSrcParticles.data[base + 3u];

  let s0 = computeSpawnParticles.data[base + 0u];
  let s1 = computeSpawnParticles.data[base + 1u];
  let s2 = computeSpawnParticles.data[base + 2u];
  let s3 = computeSpawnParticles.data[base + 3u];

  if (s1.w > 0.5) {
    let spawnKind = s3.x;
    let preserveGpuState = spawnKind > 1.5 && p1.w > 0.5;
    if (!preserveGpuState) {
      p0 = s0;
      p1 = s1;
      p2 = s2;
      p3 = s3;
    }
  }

  if (p1.w > 0.5) {
    let particleKind = p3.x;
    let stepSeconds = computeUniforms.deltaTime * computeUniforms.simulationSpeed;
    let stepLifetime = computeUniforms.deltaTime * computeUniforms.tickRate * computeUniforms.lifetimeDecay;
    if (particleKind > 2.5) {
      p1.x = p1.x + stepLifetime;
      if (p1.x >= p1.y) {
        p1.w = 0.0;
      } else {
        let dx = p0.z - p0.x;
        let dy = p0.w - p0.y;
        let distance = sqrt(max((dx * dx) + (dy * dy), 0.0));
        if (distance <= computeUniforms.mapItemReturnMinTravelDist) {
          p1.w = 0.0;
        } else {
          let moveStep = computeUniforms.mapItemReturnSpeed * computeUniforms.deltaTime * computeUniforms.tickRate;
          let ratio = min(moveStep / max(distance, 0.0001), 1.0);
          p0.x = p0.x + (dx * ratio);
          p0.y = p0.y + (dy * ratio);
        }
      }
    } else if (particleKind > 1.5) {
      p1.x = p1.x + stepLifetime;
      if (p1.x >= p1.y) {
        p1.w = 0.0;
      } else {
        let projectileStep = computeUniforms.projectileSpeed * computeUniforms.deltaTime * computeUniforms.tickRate;
        p0.x = p0.x + (p0.z * projectileStep);
        p0.y = p0.y + (p0.w * projectileStep);

        // Stage 1: GPU-side projectile/player collision check.
        let playerMinX = computeUniforms.playerX;
        let playerMinY = computeUniforms.playerY;
        let playerMaxX = playerMinX + max(computeUniforms.playerWidth, 1.0);
        let playerMaxY = playerMinY + max(computeUniforms.playerHeight, 1.0);
        let nearestX = clamp(p0.x, playerMinX, playerMaxX);
        let nearestY = clamp(p0.y, playerMinY, playerMaxY);
        let dx = p0.x - nearestX;
        let dy = p0.y - nearestY;
        let projectileRadius = max(p1.z * 0.5, 1.0);
        if ((dx * dx) + (dy * dy) <= (projectileRadius * projectileRadius)) {
          // For now mark projectile inactive when a hit is detected.
          p1.w = 0.0;
          let previousHitCount = atomicAdd(&collisionSignal.projectileHitCount, 1u);
          if (previousHitCount == 0u) {
            atomicStore(&collisionSignal.projectileHitXBits, bitcast<u32>(p0.x));
            atomicStore(&collisionSignal.projectileHitYBits, bitcast<u32>(p0.y));
            atomicStore(&collisionSignal.projectileHitSizeBits, bitcast<u32>(max(p1.z, 1.0)));
          }
        }
      }
    } else if (particleKind > 0.5) {
      let dustStep = computeUniforms.deltaTime * computeUniforms.tickRate;
      if (p1.x >= p1.y) {
        p1.w = 0.0;
      } else {
        let dustGrowth = computeUniforms.dustGrowthPerTick * dustStep;
        p1.z = min(p1.z + dustGrowth, 40.0);
        p1.x = p1.x + 1.0;
      }
    } else {
      p1.x = p1.x + stepLifetime;
      if (p1.x >= p1.y) {
        p1.w = 0.0;
      } else {
        p0.x = p0.x + (p0.z * stepSeconds);
        p0.y = p0.y + (p0.w * stepSeconds);
        p0.w = p0.w + (computeUniforms.gravity * computeUniforms.gravityBoost * stepSeconds);
      }
    }
  }

  computeDstParticles.data[base + 0u] = p0;
  computeDstParticles.data[base + 1u] = p1;
  computeDstParticles.data[base + 2u] = p2;
  computeDstParticles.data[base + 3u] = p3;
}

struct RenderUniforms {
  viewport: vec4<f32>,
  renderScale: vec4<f32>,
};

struct VertexOut {
  @builtin(position) position: vec4<f32>,
  @location(0) color: vec4<f32>,
  @location(1) quadCoord: vec2<f32>,
  @location(2) shapeFlag: f32,
  @location(3) uv: vec2<f32>,
  @location(4) particleKind: f32,
};

@group(0) @binding(0) var<storage, read> renderParticles: ParticleBuffer;
@group(0) @binding(1) var<uniform> renderUniforms: RenderUniforms;
@group(0) @binding(2) var renderSampler: sampler;
@group(0) @binding(3) var renderTexture: texture_2d<f32>;

fn quadCorner(vertexIndex: u32) -> vec2<f32> {
  switch(vertexIndex) {
    case 0u: { return vec2<f32>(-1.0, -1.0); }
    case 1u: { return vec2<f32>( 1.0, -1.0); }
    case 2u: { return vec2<f32>( 1.0,  1.0); }
    case 3u: { return vec2<f32>(-1.0, -1.0); }
    case 4u: { return vec2<f32>( 1.0,  1.0); }
    default: { return vec2<f32>(-1.0,  1.0); }
  }
}

@vertex
fn vertexMain(
  @builtin(vertex_index) vertexIndex: u32,
  @builtin(instance_index) instanceIndex: u32
) -> VertexOut {
  let base = instanceIndex * 4u;
  let p0 = renderParticles.data[base + 0u];
  let p1 = renderParticles.data[base + 1u];
  let p2 = renderParticles.data[base + 2u];
  let p3 = renderParticles.data[base + 3u];

  var out: VertexOut;
  if (p1.w <= 0.5) {
    out.position = vec4<f32>(-2.0, -2.0, 0.0, 1.0);
    out.color = vec4<f32>(0.0, 0.0, 0.0, 0.0);
    out.quadCoord = vec2<f32>(0.0, 0.0);
    out.shapeFlag = 0.0;
    out.uv = vec2<f32>(0.0, 0.0);
    out.particleKind = 0.0;
    return out;
  }

  let scale = renderUniforms.renderScale.x;
  let sizeScale = renderUniforms.renderScale.y;
  let particleKind = p3.x;
  let isMapItemReturn = particleKind > 2.5;
  let isDust = particleKind > 0.5 && particleKind <= 1.5;
  let usesUnscaledSize = isMapItemReturn || isDust;
  let widthScale = select(sizeScale, 1.0, usesUnscaledSize);
  let baseHeight = select(p1.z, max(p3.z, 1.0), isMapItemReturn);
  let halfWidth = max(p1.z * widthScale * scale * 0.5, 1.0);
  let halfHeight = max(baseHeight * widthScale * scale * 0.5, 1.0);
  let corner = quadCorner(vertexIndex);
  let localX = ((p0.x - renderUniforms.viewport.x) * scale) + (corner.x * halfWidth);
  let localY = ((p0.y - renderUniforms.viewport.y) * scale) + (corner.y * halfHeight);
  let x = (localX / renderUniforms.viewport.z) * 2.0 - 1.0;
  let y = (localY / renderUniforms.viewport.w) * 2.0 - 1.0;
  let lifetime = max(p1.y, 1.0);
  let ageProgress = clamp(p1.x / lifetime, 0.0, 1.0);
  var alphaMultiplier = 1.0;
  if (particleKind > 1.5) {
    alphaMultiplier = 1.0;
  } else if (ageProgress < 0.33) {
    alphaMultiplier = 1.0;
  } else if (ageProgress < 0.66) {
    alphaMultiplier = 0.66;
  } else {
    alphaMultiplier = 0.33;
  }

  out.position = vec4<f32>(x, -y, 0.0, 1.0);
  out.color = vec4<f32>(p2.x, p2.y, p2.z, p2.w * alphaMultiplier);
  out.quadCoord = corner;
  out.shapeFlag = p3.y;
  out.uv = vec2<f32>((corner.x + 1.0) * 0.5, (corner.y + 1.0) * 0.5);
  out.particleKind = particleKind;
  return out;
}

fn shouldDiscardCircle(shapeFlag: f32, quadCoord: vec2<f32>) -> bool {
  return shapeFlag > 0.5 && dot(quadCoord, quadCoord) > 1.0;
}

@fragment
fn fragmentMainNonDust(
  @location(0) color: vec4<f32>,
  @location(1) quadCoord: vec2<f32>,
  @location(2) shapeFlag: f32,
  @location(3) uv: vec2<f32>,
  @location(4) particleKind: f32
) -> @location(0) vec4<f32> {
  let isDust = particleKind > 0.5 && particleKind <= 1.5;
  if (isDust) {
    return vec4<f32>(0.0, 0.0, 0.0, 0.0);
  }
  if (particleKind > 2.5) {
    let sampled = textureSampleLevel(renderTexture, renderSampler, uv, 0.0);
    return sampled * color;
  }
  if (shouldDiscardCircle(shapeFlag, quadCoord)) {
    return vec4<f32>(0.0, 0.0, 0.0, 0.0);
  }
  return color;
}

@fragment
fn fragmentMainDust(
  @location(0) color: vec4<f32>,
  @location(1) quadCoord: vec2<f32>,
  @location(2) shapeFlag: f32,
  @location(3) uv: vec2<f32>,
  @location(4) particleKind: f32
) -> @location(0) vec4<f32> {
  // Keep texture/sampler bindings in this entry point so render bind group layout
  // remains consistent with the non-dust pipeline layout.
  let _layoutAnchorSample = textureSampleLevel(renderTexture, renderSampler, uv, 0.0);
  let isDust = particleKind > 0.5 && particleKind <= 1.5;
  if (!isDust) {
    return vec4<f32>(0.0, 0.0, 0.0, 0.0);
  }
  if (shouldDiscardCircle(shapeFlag, quadCoord)) {
    return vec4<f32>(0.0, 0.0, 0.0, 0.0);
  }
  // Dust uses max blending in a separate pass; return premultiplied color so
  // low-alpha dust does not appear as fully saturated opaque color.
  return vec4<f32>(color.rgb * color.a, color.a);
}

