package com.github.adamyork.sparrow.wasm.service.v1

import com.github.adamyork.sparrow.wasm.AppScope
import com.github.adamyork.sparrow.wasm.common.v1.DefaultAudioQueue
import com.github.adamyork.sparrow.wasm.common.data.Sounds
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

    @OptIn(ExperimentalWasmJsInterop::class)
    override fun playNext() {
        val nextSound = audioQueue.queue.firstOrNull() ?: return
        val elementId = when (nextSound) {
            Sounds.JUMP -> "jump-audio"
            Sounds.ITEM_COLLECT -> "collect-audio"
            Sounds.PLAYER_COLLISION -> "collision-audio"
            Sounds.ENEMY_SHOOT -> "shoot-audio"
        }
        val audioTag = document.getElementById(elementId) as? HTMLAudioElement ?: return
        if (!audioTag.paused) {
            return
        }
        audioTag.currentTime = 0.0
        audioTag.src = assetService.getAudio(nextSound)
        try {
            audioTag.play()
            audioQueue.queue.removeFirstOrNull()
        } catch (e: Throwable) {
            logger.error { "Execution error: $e" }
        }
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    override fun playBackgroundAudio() {
        logger.info { "play background called" }
        val audioUrl = assetService.getBackgroundAudio()
        val audioTag = document.getElementById("background-audio") as? HTMLAudioElement
        if (audioTag != null) {
            audioTag.src = audioUrl
            audioTag.play()
        }
    }
}
