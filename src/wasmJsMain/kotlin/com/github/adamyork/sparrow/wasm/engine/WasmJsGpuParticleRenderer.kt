package com.github.adamyork.sparrow.wasm.engine

import com.github.adamyork.sparrow.platform.common.data.ViewPort
import com.github.adamyork.sparrow.wasm.common.GPUBindGroup
import com.github.adamyork.sparrow.wasm.common.GPUBuffer
import com.github.adamyork.sparrow.wasm.common.GPUCanvasContext
import com.github.adamyork.sparrow.wasm.common.GPUCommandBuffer
import com.github.adamyork.sparrow.wasm.common.GPUCommandEncoder
import com.github.adamyork.sparrow.wasm.common.GPUComputePassEncoder
import com.github.adamyork.sparrow.wasm.common.GPUComputePipeline
import com.github.adamyork.sparrow.wasm.common.GPUDevice
import com.github.adamyork.sparrow.wasm.common.GPUQueue
import com.github.adamyork.sparrow.wasm.common.GPURenderPassEncoder
import com.github.adamyork.sparrow.wasm.common.GPURenderPipeline
import com.github.adamyork.sparrow.wasm.common.GPUSampler
import com.github.adamyork.sparrow.wasm.common.GPUTextureView
import com.github.adamyork.sparrow.wasm.common.beginComputePass
import com.github.adamyork.sparrow.wasm.common.beginRenderPass
import com.github.adamyork.sparrow.wasm.common.configureWebGpuContext
import com.github.adamyork.sparrow.wasm.common.createCommandEncoder
import com.github.adamyork.sparrow.wasm.common.createComputeBindGroup
import com.github.adamyork.sparrow.wasm.common.createLinearSampler
import com.github.adamyork.sparrow.wasm.common.createComputePipeline
import com.github.adamyork.sparrow.wasm.common.createRenderBindGroup
import com.github.adamyork.sparrow.wasm.common.createRenderPipeline
import com.github.adamyork.sparrow.wasm.common.createShaderModule
import com.github.adamyork.sparrow.wasm.common.createSolidTextureView
import com.github.adamyork.sparrow.wasm.common.createStorageBuffer
import com.github.adamyork.sparrow.wasm.common.createTextureViewFromEncodedBytes
import com.github.adamyork.sparrow.wasm.common.createUniformBuffer
import com.github.adamyork.sparrow.wasm.common.dispatchCompute
import com.github.adamyork.sparrow.wasm.common.drawRenderPassInstanced
import com.github.adamyork.sparrow.wasm.common.endComputePass
import com.github.adamyork.sparrow.wasm.common.endRenderPass
import com.github.adamyork.sparrow.wasm.common.finishCommandEncoder
import com.github.adamyork.sparrow.wasm.common.getCurrentTextureView
import com.github.adamyork.sparrow.wasm.common.getGpuQueue
import com.github.adamyork.sparrow.wasm.common.getNavigatorGpu
import com.github.adamyork.sparrow.wasm.common.getPreferredCanvasFormat
import com.github.adamyork.sparrow.wasm.common.getWebGpuContext
import com.github.adamyork.sparrow.wasm.common.hasWebGpuSupport
import com.github.adamyork.sparrow.wasm.common.queueWriteBuffer
import com.github.adamyork.sparrow.wasm.common.requestGpuAdapter
import com.github.adamyork.sparrow.wasm.common.requestGpuDevice
import com.github.adamyork.sparrow.wasm.common.setComputeBindGroup
import com.github.adamyork.sparrow.wasm.common.setComputePipeline
import com.github.adamyork.sparrow.wasm.common.setRenderBindGroup
import com.github.adamyork.sparrow.wasm.common.setRenderPipeline
import com.github.adamyork.sparrow.wasm.common.submitCommandBuffer
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.DataView
import org.khronos.webgl.toInt8Array
import org.w3c.dom.HTMLCanvasElement
import kotlin.math.ceil
import kotlin.math.max

/**
 * WebGPU collision-particle renderer used by the wasm GPU engine.
 * Physics and rendering are both performed on the GPU via compute + render pipelines.
 */
@OptIn(ExperimentalWasmJsInterop::class)
class WasmJsGpuParticleRenderer {

    companion object {
        private const val WORKGROUP_SIZE = 64
        private const val FLOATS_PER_PARTICLE = 16
        private const val BYTES_PER_FLOAT = 4
        private const val COMPUTE_UNIFORM_FLOATS = 16
        private const val RENDER_UNIFORM_FLOATS = 8
        private const val COMPUTE_ENTRY_POINT = "computeMain"
        private const val VERTEX_ENTRY_POINT = "vertexMain"
        private const val FRAGMENT_ENTRY_POINT = "fragmentMain"
    }

    private val logger = KotlinLogging.logger {}

    private var canvas: HTMLCanvasElement? = null
    private var maxParticles: Int = 0
    private var initialized: Boolean = false
    private var useStateAAsSource: Boolean = true

    private var device: GPUDevice? = null
    private var context: GPUCanvasContext? = null
    private var queue: GPUQueue? = null
    private var presentationFormat: String = ""
    private var computeWorkgroupCount: Int = 1
    private var lastConfiguredCanvasWidth: Int = -1
    private var lastConfiguredCanvasHeight: Int = -1

    private var stateBufferA: GPUBuffer? = null
    private var stateBufferB: GPUBuffer? = null
    private var spawnBuffer: GPUBuffer? = null
    private var computeUniformBuffer: GPUBuffer? = null
    private var renderUniformBuffer: GPUBuffer? = null
    private var mapItemSampler: GPUSampler? = null
    private var mapItemTextureView: GPUTextureView? = null

    private var computePipeline: GPUComputePipeline? = null
    private var renderPipeline: GPURenderPipeline? = null

    private var computeBindGroupAtoB: GPUBindGroup? = null
    private var computeBindGroupBtoA: GPUBindGroup? = null
    private var renderBindGroupA: GPUBindGroup? = null
    private var renderBindGroupB: GPUBindGroup? = null

    private val computeUniformData = FloatArray(COMPUTE_UNIFORM_FLOATS)
    private val renderUniformData = FloatArray(RENDER_UNIFORM_FLOATS)
    private var uploadScratchBuffer: ArrayBuffer? = null
    private var uploadScratchView: DataView? = null

    suspend fun initialize(
        maxParticleCapacity: Int,
        particlesShaderSource: String,
        overlayCanvas: HTMLCanvasElement,
        mapItemTextureBytes: ByteArray
    ): Boolean {
        if (initialized) return true
        if (!hasWebGpuSupport()) {
            logger.error { "WebGPU is unavailable in this browser/runtime" }
            return false
        }
        if (particlesShaderSource.isBlank()) {
            logger.error { "particles.wgsl source is empty" }
            return false
        }

        canvas = overlayCanvas
        maxParticles = maxParticleCapacity.coerceAtLeast(1)

        val gpu = getNavigatorGpu()
        val adapter = requestGpuAdapter(gpu).await() ?: run {
            logger.error { "WebGPU adapter request returned null" }
            return false
        }
        val gpuDevice = requestGpuDevice(adapter).await()
        val webGpuContext = getWebGpuContext(overlayCanvas) ?: run {
            logger.error { "Unable to acquire webgpu canvas context" }
            return false
        }

        val preferredFormat = getPreferredCanvasFormat(gpu)
        configureWebGpuContext(webGpuContext, gpuDevice, preferredFormat)

        val particleBufferSize = maxParticles * FLOATS_PER_PARTICLE * BYTES_PER_FLOAT
        val computeUniformBytes = COMPUTE_UNIFORM_FLOATS * BYTES_PER_FLOAT
        val renderUniformBytes = RENDER_UNIFORM_FLOATS * BYTES_PER_FLOAT
        computeWorkgroupCount = ceil(maxParticles.toDouble() / WORKGROUP_SIZE.toDouble()).toInt().coerceAtLeast(1)

        val createdStateA = createStorageBuffer(gpuDevice, particleBufferSize)
        val createdStateB = createStorageBuffer(gpuDevice, particleBufferSize)
        val createdSpawn = createStorageBuffer(gpuDevice, particleBufferSize)
        val createdComputeUniform = createUniformBuffer(gpuDevice, computeUniformBytes)
        val createdRenderUniform = createUniformBuffer(gpuDevice, renderUniformBytes)
        val createdMapItemSampler = createLinearSampler(gpuDevice)
        val deviceQueue = getGpuQueue(gpuDevice)
        val createdMapItemTextureView = if (mapItemTextureBytes.isNotEmpty()) {
            try {
                createTextureViewFromEncodedBytes(gpuDevice, deviceQueue, mapItemTextureBytes.toInt8Array()).await()
            } catch (t: Throwable) {
                logger.error(t) { "Failed to create map item return texture for WebGPU particles" }
                return false
            }
        } else {
            createSolidTextureView(gpuDevice, deviceQueue, 255, 255, 255, 255)
        }

        val shaderModule = createShaderModule(gpuDevice, particlesShaderSource)

        val createdComputePipeline = createComputePipeline(gpuDevice, shaderModule, COMPUTE_ENTRY_POINT)
        val createdRenderPipeline = createRenderPipeline(
            gpuDevice,
            shaderModule,
            shaderModule,
            preferredFormat,
            VERTEX_ENTRY_POINT,
            FRAGMENT_ENTRY_POINT
        )

        val bindGroupAToB = createComputeBindGroup(
            gpuDevice,
            createdComputePipeline,
            createdStateA,
            createdStateB,
            createdSpawn,
            createdComputeUniform
        )
        val bindGroupBToA = createComputeBindGroup(
            gpuDevice,
            createdComputePipeline,
            createdStateB,
            createdStateA,
            createdSpawn,
            createdComputeUniform
        )
        val createdRenderBindGroupA = createRenderBindGroup(
            gpuDevice,
            createdRenderPipeline,
            createdStateA,
            createdRenderUniform,
            createdMapItemSampler,
            createdMapItemTextureView
        )
        val createdRenderBindGroupB = createRenderBindGroup(
            gpuDevice,
            createdRenderPipeline,
            createdStateB,
            createdRenderUniform,
            createdMapItemSampler,
            createdMapItemTextureView
        )

        device = gpuDevice
        context = webGpuContext
        queue = deviceQueue
        presentationFormat = preferredFormat
        stateBufferA = createdStateA
        stateBufferB = createdStateB
        spawnBuffer = createdSpawn
        computeUniformBuffer = createdComputeUniform
        renderUniformBuffer = createdRenderUniform
        mapItemSampler = createdMapItemSampler
        mapItemTextureView = createdMapItemTextureView
        computePipeline = createdComputePipeline
        renderPipeline = createdRenderPipeline
        computeBindGroupAtoB = bindGroupAToB
        computeBindGroupBtoA = bindGroupBToA
        renderBindGroupA = createdRenderBindGroupA
        renderBindGroupB = createdRenderBindGroupB
        initialized = true
        logger.info { "WebGPU particle renderer initialized with $maxParticles slots" }
        return true
    }

    fun updateGpuParticleBuffer(
        activeParticleCount: Int,
        sourceBuffer: FloatArray,
        dirtySlotRanges: List<IntRange>,
        deltaTimeSeconds: Float,
        gravity: Float,
        tickTargetPerSecond: Int,
        speedCoefficient: Float,
        dustSpeedCoefficient: Float,
        projectileSpeed: Float,
        mapItemReturnSpeed: Float,
        mapItemReturnMinTravelDist: Float
    ) {
        check(initialized) { "WebGPU renderer must be initialized before updateGpuParticleBuffer" }
        val gpuDevice = device ?: return
        val gpuQueue = queue ?: return
        val spawn = spawnBuffer ?: return
        val computeUniform = computeUniformBuffer ?: return
        val pipeline = computePipeline ?: return
        val bindGroup = if (useStateAAsSource) computeBindGroupAtoB else computeBindGroupBtoA
        if (bindGroup == null) return

        val clampedSpawnCount = activeParticleCount.coerceIn(0, maxParticles)
        for (slotRange in dirtySlotRanges) {
            val clampedStartSlot = slotRange.first.coerceIn(0, maxParticles)
            val clampedEndExclusive = (slotRange.last + 1).coerceIn(clampedStartSlot, maxParticles)
            val slotCount = clampedEndExclusive - clampedStartSlot
            if (slotCount <= 0) continue
            val rangeStartFloat = clampedStartSlot * FLOATS_PER_PARTICLE
            val rangeFloatCount = slotCount * FLOATS_PER_PARTICLE
            val targetOffsetBytes = rangeStartFloat * BYTES_PER_FLOAT
            writeFloatArrayBufferRange(
                queue = gpuQueue,
                targetBuffer = spawn,
                data = sourceBuffer,
                sourceStartFloat = rangeStartFloat,
                floatCount = rangeFloatCount,
                targetOffsetBytes = targetOffsetBytes
            )
        }

        val tunedSpeed = speedCoefficient.coerceAtLeast(0.05f)
        val simulationSpeed = (1f + (tunedSpeed * 8f)).coerceAtLeast(1f)
        val gravityBoost = (1.5f + (tunedSpeed * 6f)).coerceAtLeast(1f)
        val lifetimeDecay = (1f + (tunedSpeed * 6f)).coerceAtLeast(1f)
        val dustGrowthPerTick = dustSpeedCoefficient.coerceAtLeast(0f)
        computeUniformData[0] = deltaTimeSeconds.coerceAtLeast(0.0001f)
        computeUniformData[1] = gravity
        computeUniformData[2] = clampedSpawnCount.toFloat()
        computeUniformData[3] = maxParticles.toFloat()
        computeUniformData[4] = tickTargetPerSecond.coerceAtLeast(1).toFloat()
        computeUniformData[5] = simulationSpeed
        computeUniformData[6] = gravityBoost
        computeUniformData[7] = lifetimeDecay
        computeUniformData[8] = dustGrowthPerTick
        computeUniformData[9] = projectileSpeed.coerceAtLeast(0f)
        computeUniformData[10] = mapItemReturnSpeed.coerceAtLeast(0f)
        computeUniformData[11] = mapItemReturnMinTravelDist.coerceAtLeast(0f)
        computeUniformData[12] = 0f
        computeUniformData[13] = 0f
        computeUniformData[14] = 0f
        computeUniformData[15] = 0f
        writeFloatArrayBuffer(gpuQueue, computeUniform, computeUniformData)

        val encoder: GPUCommandEncoder = createCommandEncoder(gpuDevice)
        val computePass: GPUComputePassEncoder = beginComputePass(encoder)
        setComputePipeline(computePass, pipeline)
        setComputeBindGroup(computePass, bindGroup)
        dispatchCompute(computePass, computeWorkgroupCount)
        endComputePass(computePass)

        val commandBuffer: GPUCommandBuffer = finishCommandEncoder(encoder)
        submitCommandBuffer(gpuQueue, commandBuffer)
        useStateAAsSource = !useStateAAsSource
    }

    fun draw(viewPort: ViewPort, sizeMultiplier: Int) {
        check(initialized) { "WebGPU renderer must be initialized before draw" }
        val gpuDevice = device ?: return
        val webGpuContext = context ?: return
        val gpuQueue = queue ?: return
        val pipeline = renderPipeline ?: return
        val targetCanvas = canvas ?: return
        if (targetCanvas.width <= 0 || targetCanvas.height <= 0) return
        if (targetCanvas.width != lastConfiguredCanvasWidth || targetCanvas.height != lastConfiguredCanvasHeight) {
            configureWebGpuContext(webGpuContext, gpuDevice, presentationFormat)
            lastConfiguredCanvasWidth = targetCanvas.width
            lastConfiguredCanvasHeight = targetCanvas.height
        }

        val sizeScale = (sizeMultiplier.coerceAtLeast(1).toFloat() / 14f).coerceAtLeast(0.1f)
        renderUniformData[0] = viewPort.x.toFloat()
        renderUniformData[1] = viewPort.y.toFloat()
        renderUniformData[2] = targetCanvas.width.toFloat()
        renderUniformData[3] = targetCanvas.height.toFloat()
        renderUniformData[4] = getRenderScale()
        renderUniformData[5] = sizeScale
        renderUniformData[6] = 0f
        renderUniformData[7] = 0f
        val renderUniform = renderUniformBuffer ?: return
        writeFloatArrayBuffer(gpuQueue, renderUniform, renderUniformData)

        val textureView: GPUTextureView = getCurrentTextureView(webGpuContext)
        val renderBindGroup = if (useStateAAsSource) renderBindGroupA else renderBindGroupB
        if (renderBindGroup == null) return

        val encoder: GPUCommandEncoder = createCommandEncoder(gpuDevice)
        val renderPass: GPURenderPassEncoder = beginRenderPass(encoder, textureView)
        setRenderPipeline(renderPass, pipeline)
        setRenderBindGroup(renderPass, renderBindGroup)
        drawRenderPassInstanced(renderPass, 6, maxParticles)
        endRenderPass(renderPass)

        val commandBuffer: GPUCommandBuffer = finishCommandEncoder(encoder)
        submitCommandBuffer(gpuQueue, commandBuffer)
    }

    private fun writeFloatArrayBuffer(queue: GPUQueue, targetBuffer: GPUBuffer, data: FloatArray) {
        writeFloatArrayBufferRange(
            queue = queue,
            targetBuffer = targetBuffer,
            data = data,
            sourceStartFloat = 0,
            floatCount = data.size,
            targetOffsetBytes = 0
        )
    }

    private fun writeFloatArrayBufferRange(
        queue: GPUQueue,
        targetBuffer: GPUBuffer,
        data: FloatArray,
        sourceStartFloat: Int,
        floatCount: Int,
        targetOffsetBytes: Int
    ) {
        if (floatCount <= 0 || sourceStartFloat < 0 || (sourceStartFloat + floatCount) > data.size) return
        val requiredBytes = floatCount * BYTES_PER_FLOAT
        val (buffer, view) = getOrCreateUploadScratch(requiredBytes)
        var i = 0
        while (i < floatCount) {
            view.setFloat32(i * BYTES_PER_FLOAT, data[sourceStartFloat + i], true)
            i++
        }
        queueWriteBuffer(queue, targetBuffer, targetOffsetBytes, buffer, requiredBytes)
    }

    // Reuse host upload memory to avoid per-frame ArrayBuffer/DataView allocations.
    private fun getOrCreateUploadScratch(requiredBytes: Int): Pair<ArrayBuffer, DataView> {
        val currentBuffer = uploadScratchBuffer
        val currentView = uploadScratchView
        if (currentBuffer != null && currentView != null && currentBuffer.byteLength >= requiredBytes) {
            return currentBuffer to currentView
        }
        val resizedBuffer = ArrayBuffer(max(requiredBytes, 1))
        val resizedView = DataView(resizedBuffer)
        uploadScratchBuffer = resizedBuffer
        uploadScratchView = resizedView
        return resizedBuffer to resizedView
    }

    private fun getRenderScale(): Float = window.devicePixelRatio.toFloat().coerceAtLeast(1f)


}
