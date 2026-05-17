package de.sudokuonline.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Sound Manager for game audio effects.
 * Provides various sound effects for game events.
 */
class SoundManager private constructor(context: Context) {

    private var soundPool: SoundPool? = null
    private var isEnabled = true

    // Sound IDs
    private var soundTap = 0
    private var soundSuccess = 0
    private var soundError = 0
    private var soundWin = 0
    private var soundLose = 0
    private var soundHint = 0
    private var soundUndo = 0
    private var soundGameStart = 0
    private var soundBomb = 0
    private var soundPlace = 0

    init {
        initSoundPool(context)
    }

    private fun initSoundPool(context: Context) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        // Load sounds - using system sounds as fallback
        // These will be silent if the raw resources don't exist
        try {
            // We'll generate simple tones programmatically
            soundPool?.let { pool ->
                // For now, we'll use the pool ID as placeholder
                // In production, load actual sound files from res/raw
                soundTap = 1
                soundSuccess = 2
                soundError = 3
                soundWin = 4
                soundLose = 5
                soundHint = 6
                soundUndo = 7
                soundGameStart = 8
                soundBomb = 9
                soundPlace = 10
            }
        } catch (e: Exception) {
            // Sound loading failed, sounds will be disabled
        }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun isEnabled(): Boolean = isEnabled

    /**
     * Play tap sound (cell selection, button press)
     */
    fun playTap() {
        if (!isEnabled) return
        playTone(800f, 50)
    }

    /**
     * Play number placement sound
     */
    fun playPlace() {
        if (!isEnabled) return
        playTone(1000f, 80)
    }

    /**
     * Play success sound (correct move, forming mill)
     */
    fun playSuccess() {
        if (!isEnabled) return
        playToneSequence(listOf(800f to 100, 1200f to 150))
    }

    /**
     * Play error sound (wrong number, invalid move)
     */
    fun playError() {
        if (!isEnabled) return
        playTone(300f, 200)
    }

    /**
     * Play win sound
     */
    fun playWin() {
        if (!isEnabled) return
        playToneSequence(listOf(
            523f to 150,  // C5
            659f to 150,  // E5
            784f to 150,  // G5
            1047f to 300  // C6
        ))
    }

    /**
     * Play lose sound
     */
    fun playLose() {
        if (!isEnabled) return
        playToneSequence(listOf(
            400f to 200,
            350f to 200,
            300f to 400
        ))
    }

    /**
     * Play hint sound
     */
    fun playHint() {
        if (!isEnabled) return
        playToneSequence(listOf(600f to 100, 900f to 100))
    }

    /**
     * Play undo sound
     */
    fun playUndo() {
        if (!isEnabled) return
        playTone(500f, 100)
    }

    /**
     * Play game start sound
     */
    fun playGameStart() {
        if (!isEnabled) return
        playToneSequence(listOf(
            523f to 100,
            659f to 100,
            784f to 200
        ))
    }

    /**
     * Play bomb/explosion sound
     */
    fun playBomb() {
        if (!isEnabled) return
        playTone(150f, 300)
    }

    /**
     * Play countdown tick
     */
    fun playTick() {
        if (!isEnabled) return
        playTone(1000f, 30)
    }

    /**
     * Play lootbox shake/rattle sound (low rumble building up)
     */
    fun playLootboxShake() {
        if (!isEnabled) return
        playToneSequence(listOf(
            200f to 120,
            250f to 120,
            300f to 100,
            350f to 100,
            400f to 80,
            500f to 80,
            600f to 60,
            700f to 60
        ))
    }

    /**
     * Play lootbox burst open sound (explosive reveal)
     */
    fun playLootboxBurst() {
        if (!isEnabled) return
        playToneSequence(listOf(
            150f to 80,
            800f to 60,
            1200f to 100,
            1600f to 150
        ))
    }

    /**
     * Play reward reveal sound (sparkle chime)
     */
    fun playRewardReveal() {
        if (!isEnabled) return
        playToneSequence(listOf(
            880f to 80,
            1100f to 80,
            1320f to 120
        ))
    }

    /**
     * Play rare/epic reward reveal (dramatic fanfare)
     */
    fun playRareReveal() {
        if (!isEnabled) return
        playToneSequence(listOf(
            523f to 120,
            659f to 120,
            784f to 120,
            1047f to 200,
            1319f to 300
        ))
    }

    private fun playTone(frequency: Float, durationMs: Int) {
        // Using Android's ToneGenerator as fallback
        try {
            val toneGenerator = android.media.ToneGenerator(
                android.media.AudioManager.STREAM_MUSIC,
                50 // volume
            )
            // Map frequency to tone type (approximation)
            val toneType = when {
                frequency < 400 -> android.media.ToneGenerator.TONE_PROP_BEEP
                frequency < 700 -> android.media.ToneGenerator.TONE_PROP_ACK
                frequency < 1000 -> android.media.ToneGenerator.TONE_PROP_PROMPT
                else -> android.media.ToneGenerator.TONE_PROP_BEEP2
            }
            toneGenerator.startTone(toneType, durationMs)
            // Release after playing
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                toneGenerator.release()
            }, durationMs.toLong() + 50)
        } catch (e: Exception) {
            // Tone generation failed, ignore
        }
    }

    private fun playToneSequence(tones: List<Pair<Float, Int>>) {
        var delay = 0L
        for ((frequency, duration) in tones) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                playTone(frequency, duration)
            }, delay)
            delay += duration
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
    }

    companion object {
        @Volatile
        private var instance: SoundManager? = null

        fun getInstance(context: Context): SoundManager {
            return instance ?: synchronized(this) {
                instance ?: SoundManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

/**
 * Composable to get SoundManager
 */
@Composable
fun rememberSoundManager(): SoundManager {
    val context = LocalContext.current
    return remember { SoundManager.getInstance(context) }
}
