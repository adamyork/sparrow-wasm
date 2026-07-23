package com.github.adamyork.sparrow.wasm.service.v1

import com.github.adamyork.sparrow.platform.service.WavService
import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.common.AudioQueue
import com.github.adamyork.sparrow.platform.service.AssetService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.browser.document
import me.tatarka.inject.annotations.Inject
import org.w3c.dom.HTMLAudioElement

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@OptIn(ExperimentalWasmJsInterop::class)
@AppScope
@Inject
class WasmWavService(
    private val assetService: AssetService,
    private val audioQueue: AudioQueue
) : WavService {

    private val logger = KotlinLogging.logger {}

    private companion object {
        private const val BG_AUDIO_ID = "background-audio"
        private const val SFX_POOL_SIZE = 6
    }

    private val sfxPool: List<HTMLAudioElement> = List(SFX_POOL_SIZE) { createSfxAudioElement() }

    override fun playNext() {
        val nextSound = audioQueue.queue.firstOrNull() ?: return
        val availableAudio = sfxPool.firstOrNull { it.paused } ?: return
        availableAudio.currentTime = 0.0
        availableAudio.src = assetService.getAudioPath(nextSound)
        runCatching { availableAudio.play() }
            .onSuccess { audioQueue.queue.removeFirstOrNull() }
            .onFailure { error ->
                logger.error(error) { "Failed to play queued sound: $nextSound" }
            }
    }

    override fun playBackgroundAudio() {
        val audioTag = document.getElementById(BG_AUDIO_ID) as? HTMLAudioElement ?: run {
            return
        }
        audioTag.src = assetService.getBackgroundAudio()
        runCatching { audioTag.play() }
            .onFailure { failure ->
                logger.error(failure) { "Failed to start background audio" }
            }
    }

    private fun createSfxAudioElement(): HTMLAudioElement {
        val element = document.createElement("audio").unsafeCast<HTMLAudioElement>()
        element.preload = "auto"
        return element
    }
}
