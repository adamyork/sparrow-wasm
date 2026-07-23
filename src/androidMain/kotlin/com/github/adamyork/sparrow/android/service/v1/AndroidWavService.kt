package com.github.adamyork.sparrow.android.service.v1

import android.media.AudioAttributes
import android.media.MediaPlayer
import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.common.AudioQueue
import com.github.adamyork.sparrow.platform.service.AssetService
import com.github.adamyork.sparrow.platform.service.WavService
import io.github.oshai.kotlinlogging.KotlinLogging
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class AndroidWavService(
    private val assetService: AssetService,
    private val audioQueue: AudioQueue
) : WavService {

    private val logger = KotlinLogging.logger {}

    private companion object {
        private const val SFX_POOL_SIZE = 6
    }

    private val sfxPool: List<MediaPlayer> = List(SFX_POOL_SIZE) { createSfxPlayer() }
    private val backgroundPlayer: MediaPlayer = createBackgroundPlayer()

	override fun playNext() {
		val nextSound = audioQueue.queue.firstOrNull() ?: return
		val availablePlayer = sfxPool.firstOrNull { player ->
			runCatching { !player.isPlaying }.getOrElse { failure ->
				logger.error(failure) { "Failed to inspect MediaPlayer state" }
				false
			}
		} ?: return
		runCatching {
			availablePlayer.reset()
			availablePlayer.setAudioAttributes(defaultAudioAttributes())
			availablePlayer.setDataSource(assetService.getAudioPath(nextSound))
			availablePlayer.prepare()
			availablePlayer.seekTo(0)
			availablePlayer.start()
		}.onSuccess {
			audioQueue.queue.removeFirstOrNull()
		}.onFailure { error ->
			logger.error(error) { "Failed to play queued sound: $nextSound" }
		}
	}

	override fun playBackgroundAudio() {
		runCatching {
			backgroundPlayer.reset()
			backgroundPlayer.setAudioAttributes(defaultAudioAttributes())
			backgroundPlayer.isLooping = true
			backgroundPlayer.setDataSource(assetService.getBackgroundAudio())
			backgroundPlayer.prepare()
			backgroundPlayer.start()
		}.onFailure { error ->
			logger.error(error) { "Failed to start background audio" }
		}
	}

	private fun createSfxPlayer(): MediaPlayer {
		return MediaPlayer().apply {
			setAudioAttributes(defaultAudioAttributes())
		}
	}

	private fun createBackgroundPlayer(): MediaPlayer {
		return MediaPlayer().apply {
			setAudioAttributes(defaultAudioAttributes())
			isLooping = true
		}
	}

	private fun defaultAudioAttributes(): AudioAttributes {
		return AudioAttributes.Builder()
			.setUsage(AudioAttributes.USAGE_GAME)
			.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
			.build()
	}
}
