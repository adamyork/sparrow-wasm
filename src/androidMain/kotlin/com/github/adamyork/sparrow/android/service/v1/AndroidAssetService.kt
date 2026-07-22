package com.github.adamyork.sparrow.android.service.v1

import android.graphics.*
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.common.data.Sounds
import com.github.adamyork.sparrow.platform.common.data.enemy.MapElementFactory
import com.github.adamyork.sparrow.platform.service.LoadingProgressListener
import com.github.adamyork.sparrow.platform.service.data.AssetServiceReferenceException
import com.github.adamyork.sparrow.platform.service.data.ImageAndBytes
import com.github.adamyork.sparrow.platform.service.data.ImageAsset
import com.github.adamyork.sparrow.platform.service.v1.AbstractPlatformAssetService
import com.github.adamyork.sparrow_wasm.generated.resources.Res
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import java.io.ByteArrayOutputStream
import java.io.File


/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
class AndroidAssetService(
    httpClient: HttpClient,
    mapElementFactory: MapElementFactory
) : AbstractPlatformAssetService(httpClient, mapElementFactory) {

    private lateinit var backgroundAudio: String

    override suspend fun initialize(listener: LoadingProgressListener) {
        logger.info { "initialize called loading yaml" }
        val bytes = Res.readBytes("files/application.yml")
        finishInit(bytes = bytes, listener = listener)
    }

    override suspend fun loadBufferedImageAsync(file: String): ImageBitmap {
        logger.info { "loadBufferedImageAsync called to load $file" }
        val response = httpClient.get(file)
        check(response.status.isSuccess()) { "Failed to load image from URL: $file (status=${response.status})" }
        logger.info { "image loaded" }
        val bytes = response.body<ByteArray>()
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: error("Failed to decode byte array into Android Bitmap")
        return bitmap.asImageBitmap()
    }

    override suspend fun loadAudio(listener: LoadingProgressListener) = coroutineScope {
        val audioPathMap = mapOf(
            Sounds.JUMP to appProperties.audio.player.jump,
            Sounds.PLAYER_COLLISION to appProperties.audio.player.collision,
            Sounds.ENEMY_SHOOT to appProperties.audio.enemy.shoot,
            Sounds.ITEM_COLLECT to appProperties.audio.item.collect
        )

        suspend fun fetchBytes(url: String): ByteArray {
            val response = httpClient.get(url)
            check(response.status.isSuccess()) { "Failed to load $url(status=${response.status})" }
            return response.body<ByteArray>()
        }

        suspend fun persistToTempAudioFile(bytes: ByteArray): String {
            return withContext(Dispatchers.IO) {
                val file = File.createTempFile("sparrow-audio-", ".wav")
                file.writeBytes(bytes)
                file.absolutePath
            }
        }

        val deferredAudios = audioPathMap.map { (key, path) ->
            key to async { fetchBytes(path) }
        }
        val deferredBackground = async { fetchBytes(appProperties.audio.background) }
        deferredAudios.forEach { (key, deferred) ->
            val blob = deferred.await()
            audioMap[key] = persistToTempAudioFile(blob)
            listener.onTaskCompleted(key.name)
        }
        listener.onTaskCompleted(appProperties.audio.background)
        backgroundAudio = persistToTempAudioFile(deferredBackground.await())
    }

    override fun getBackgroundAudio(): String = backgroundAudio

    override fun getAudioPath(sound: Sounds): String {
        return audioMap[sound] ?: throw AssetServiceReferenceException("no audio path for for key $sound")
    }

    override suspend fun fetchImageAndBytes(path: String, width: Int, height: Int): ImageAsset {
        logger.info { "fetchImageAndBytes for $path" }
        val response = httpClient.get(path)
        val bytes = response.body<ByteArray>()
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: error("Failed to decode byte array into Android Bitmap")
        val actual = bitmap.asImageBitmap()
        return ImageAsset(width, height, ImageAndBytes(bytes, actual))

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun prepareFont(): Any {
        val bytes = Res.readBytes("files/roboto_bold.ttf")
        val tempFile = withContext(Dispatchers.IO) {
            File.createTempFile("font_temp", ".ttf")
        }
        try {
            tempFile.writeBytes(bytes)
            val typeface = Typeface.Builder(tempFile).build()
                ?: throw AssetServiceReferenceException("Could not create typeface from data")
            return FontFamily(typeface)
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    override fun drawId(
        bytes: ByteArray,
        id: Int,
        frameWidth: Int,
        frameHeight: Int,
        font: Any
    ): ByteArray {
        val options = BitmapFactory.Options().apply {
            inMutable = true
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return bytes
        val textPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = 12f
            if (font is Typeface) {
                typeface = font
            }
        }
        try {
            val canvas = Canvas(bitmap)
            val safeFrameWidth = if (frameWidth > 0) frameWidth else bitmap.width
            val safeFrameHeight = if (frameHeight > 0) frameHeight else bitmap.height
            val columns = (bitmap.width / safeFrameWidth).coerceAtLeast(1)
            val rows = (bitmap.height / safeFrameHeight).coerceAtLeast(1)
            val text = id.toString()
            for (row in 0 until rows) {
                for (column in 0 until columns) {
                    val cellX = (column * safeFrameWidth).toFloat()
                    val cellY = (row * safeFrameHeight).toFloat()
                    val drawX = cellX + 5f
                    val drawY = cellY + 15f
                    canvas.drawText(text, drawX, drawY, textPaint)
                }
            }
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            return outputStream.toByteArray()
        } finally {
            bitmap.recycle()
        }
    }
}
