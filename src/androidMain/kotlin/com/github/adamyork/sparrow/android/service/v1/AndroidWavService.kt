package com.github.adamyork.sparrow.android.service.v1

import android.media.AudioAttributes
import android.media.MediaPlayer
import com.github.adamyork.sparrow.platform.AppScope
import com.github.adamyork.sparrow.platform.common.AudioQueue
import com.github.adamyork.sparrow.platform.common.data.Sounds
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

	private data class SfxSlot(
		val player: MediaPlayer,
		var loadedSound: Sounds? = null
	)

	private val audioAttributes: AudioAttributes = AudioAttributes.Builder()
		.setUsage(AudioAttributes.USAGE_GAME)
		.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
		.build()

	private val sfxPool: List<SfxSlot> = List(SFX_POOL_SIZE) { SfxSlot(createSfxPlayer()) }
    private val backgroundPlayer: MediaPlayer = createBackgroundPlayer()
	private var backgroundSource: String? = null

	override fun playNext() {
		val nextSound = audioQueue.queue.firstOrNull() ?: return
		val availableSlot = sfxPool.firstOrNull { slot ->
			runCatching { !slot.player.isPlaying }.getOrElse { failure ->
				logger.error(failure) { "Failed to inspect MediaPlayer state" }
				false
			}
		} ?: return
		runCatching {
			if (availableSlot.loadedSound != nextSound) {
				availableSlot.player.reset()
				availableSlot.player.setAudioAttributes(audioAttributes)
				availableSlot.player.setDataSource(assetService.getAudioPath(nextSound))
				availableSlot.player.prepare()
				availableSlot.loadedSound = nextSound
			} else {
				availableSlot.player.seekTo(0)
			}
			availableSlot.player.start()
		}.onSuccess {
			audioQueue.queue.removeFirstOrNull()
		}.onFailure { error ->
			availableSlot.loadedSound = null
			logger.error(error) { "Failed to play queued sound: $nextSound" }
		}
	}

	override fun playBackgroundAudio() {
		val source = assetService.getBackgroundAudio()
		val alreadyPlayingSameSource = runCatching {
			backgroundPlayer.isPlaying && backgroundSource == source
		}.getOrDefault(false)
		if (alreadyPlayingSameSource) {
			return
		}
		runCatching {
			if (backgroundSource != source) {
				backgroundPlayer.reset()
				backgroundPlayer.setAudioAttributes(audioAttributes)
				backgroundPlayer.isLooping = true
				backgroundPlayer.setDataSource(source)
				backgroundPlayer.prepare()
				backgroundSource = source
			}
			backgroundPlayer.start()
		}.onFailure { error ->
			backgroundSource = null
			logger.error(error) { "Failed to start background audio" }
		}
	}

	private fun createSfxPlayer(): MediaPlayer {
		return MediaPlayer().apply {
			setAudioAttributes(audioAttributes)
		}
	}

	private fun createBackgroundPlayer(): MediaPlayer {
		return MediaPlayer().apply {
			setAudioAttributes(audioAttributes)
			isLooping = true
		}
	}
}
