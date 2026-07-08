package com.github.adamyork.sparrow.wasm.service.v1

import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.common.data.Sounds
import com.github.adamyork.sparrow.wasm.common.v1.DefaultAudioQueue
import com.github.adamyork.sparrow.wasm.service.AssetService
import com.github.adamyork.sparrow.wasm.service.WavService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.browser.document
import me.tatarka.inject.annotations.Inject
import org.w3c.dom.HTMLAudioElement

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
@AppScope
@Inject
class DefaultWavService(
    private val assetService: AssetService,
    private val audioQueue: DefaultAudioQueue
) : WavService {

    private val logger = KotlinLogging.logger {}

    private companion object {
        private const val BG_AUDIO_ID = "background-audio"
        private val SOUND_ID_MAP = mapOf(
            Sounds.JUMP to "jump-audio",
            Sounds.ITEM_COLLECT to "collect-audio",
            Sounds.PLAYER_COLLISION to "collision-audio",
            Sounds.ENEMY_SHOOT to "shoot-audio"
        )
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    override fun playNext() {
        val nextSound = audioQueue.queue.firstOrNull() ?: return
        val elementId = SOUND_ID_MAP[nextSound] ?: return
        val audioTag = document.getElementById(elementId) as? HTMLAudioElement ?: return
        if (!audioTag.paused) return
        audioTag.currentTime = 0.0
        audioTag.src = assetService.getAudioPath(nextSound)
        runCatching {
            audioTag.play()
            audioQueue.queue.removeFirstOrNull()
        }.onFailure { e ->
            logger.error(e) { "Failed to play sound: $nextSound" }
        }
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    override fun playBackgroundAudio() {
        logger.info { "play background called" }
        val audioTag = document.getElementById(BG_AUDIO_ID) as? HTMLAudioElement ?: run {
            logger.warn { "Background audio element not found in DOM" }
            return
        }
        audioTag.src = assetService.getBackgroundAudio()
        runCatching { audioTag.play() }
            .onFailure { logger.error(it) { "Failed to start background audio" } }
    }
}
