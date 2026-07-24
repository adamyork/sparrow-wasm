package com.github.adamyork.sparrow.wasm.service.v1

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.common.PlatformInterop
import com.github.adamyork.sparrow.platform.common.data.Sounds
import com.github.adamyork.sparrow.platform.common.data.enemy.MapElementFactory
import com.github.adamyork.sparrow.platform.service.LoadingProgressListener
import com.github.adamyork.sparrow.platform.service.data.AssetServiceReferenceException
import com.github.adamyork.sparrow.platform.service.data.ImageAndBytes
import com.github.adamyork.sparrow.platform.service.data.ImageAsset
import com.github.adamyork.sparrow.platform.service.v1.AbstractPlatformAssetService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import me.tatarka.inject.annotations.Inject
import org.jetbrains.skia.*
import org.w3c.files.Blob

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
class WasmJsAssetService(
    httpClient: HttpClient,
    mapElementFactory: MapElementFactory,
    private val platformInterop: PlatformInterop
) : AbstractPlatformAssetService(httpClient, mapElementFactory) {

    private lateinit var backgroundAudio: String

    override suspend fun loadBufferedImageAsync(file: String): ImageBitmap {
        logger.debug { "HTTP GET: $file" }
        val response = httpClient.get(file)
        check(response.status.isSuccess()) { "Failed to load image from URL: $file (status=${response.status})" }
        val skiaImage = Image.makeFromEncoded(response.body<ByteArray>())
        try {
            return skiaImage.toComposeImageBitmap()
        } finally {
            skiaImage.close()
        }
    }

    override suspend fun loadAudio(listener: LoadingProgressListener) = coroutineScope {
        val audioPathMap = mapOf(
            Sounds.JUMP to appProperties.audio.player.jump,
            Sounds.PLAYER_COLLISION to appProperties.audio.player.collision,
            Sounds.ENEMY_SHOOT to appProperties.audio.enemy.shoot,
            Sounds.ITEM_COLLECT to appProperties.audio.item.collect
        )

        suspend fun fetchBlob(url: String): Blob {
            logger.debug { "HTTP GET: $url" }
            val response = httpClient.get(url)
            check(response.status.isSuccess()) { "Failed to load $url(status=${response.status})" }
            val bytes = response.body<ByteArray>()
            return platformInterop.getBlobFromBytes(bytes) as Blob
        }

        val deferredAudios = audioPathMap.map { (key, path) ->
            key to async { fetchBlob(path) }
        }
        val deferredBackground = async { fetchBlob(appProperties.audio.background) }
        deferredAudios.forEach { (key, deferred) ->
            try {
                val blob = deferred.await()
                audioMap[key] = platformInterop.createAudioBlobUri(blob)
                listener.onTaskCompleted(key.name)
            } catch (failure: Throwable) {
                listener.onTaskFailed(key.name, failure)
                throw failure
            }
        }
        try {
            backgroundAudio = platformInterop.createAudioBlobUri(deferredBackground.await())
            listener.onTaskCompleted(appProperties.audio.background)
        } catch (failure: Throwable) {
            listener.onTaskFailed(appProperties.audio.background, failure)
            throw failure
        }
    }

    override fun getBackgroundAudio(): String = backgroundAudio

    override fun getAudioPath(sound: Sounds): String {
        return audioMap[sound] ?: throw AssetServiceReferenceException("no audio path for for key $sound")
    }

    override suspend fun fetchImageAndBytes(path: String, width: Int, height: Int): ImageAsset {
        logger.debug { "HTTP GET: $path" }
        val response = httpClient.get(path)
        val bytes = response.body<ByteArray>()
        val skiaImage = Image.makeFromEncoded(bytes)
        try {
            val bitmap = skiaImage.toComposeImageBitmap()
            return ImageAsset(width, height, ImageAndBytes(bytes, bitmap))
        } finally {
            skiaImage.close()
        }
    }

    override suspend fun prepareFont(): Any {
        logger.debug { "HTTP GET: roboto_bold.ttf" }
        val response = httpClient.get("roboto_bold.ttf")
        check(response.status.isSuccess()) { "Failed to load application font (status=${response.status})" }
        val bytes = response.body<ByteArray>()
        val data = Data.makeFromBytes(bytes)
        val typeFace = FontMgr.default.makeFromData(data)
            ?: throw AssetServiceReferenceException("Could not create typeface from data")
        return Font(typeFace, 12F)

    }


    override fun drawId(
        bytes: ByteArray,
        id: Int,
        frameWidth: Int,
        frameHeight: Int,
        font: Any
    ): ByteArray {
        val image = Image.makeFromEncoded(bytes)
        val surface = Surface.makeRasterN32Premul(image.width, image.height)
        val textPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
        }
        try {
            val canvas = surface.canvas
            canvas.drawImage(image, 0f, 0f)
            val safeFrameWidth = if (frameWidth > 0) frameWidth else image.width
            val safeFrameHeight = if (frameHeight > 0) frameHeight else image.height
            val columns = (image.width / safeFrameWidth).coerceAtLeast(1)
            val rows = (image.height / safeFrameHeight).coerceAtLeast(1)
            val text = id.toString()
            for (row in 0 until rows) {
                for (column in 0 until columns) {
                    val cellX = (column * safeFrameWidth).toFloat()
                    val cellY = (row * safeFrameHeight).toFloat()
                    val drawX = cellX + 5f
                    val drawY = cellY + 15f
                    canvas.drawString(text, drawX, drawY, font as Font?, textPaint)
                }
            }
            val data = surface.makeImageSnapshot().encodeToData() ?: return bytes
            return data.bytes
        } finally {
            image.close()
            surface.close()
            textPaint.close()
        }
    }
}
