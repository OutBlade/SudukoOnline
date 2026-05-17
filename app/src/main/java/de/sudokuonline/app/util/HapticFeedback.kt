package de.sudokuonline.app.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * Haptic Feedback utility for providing tactile feedback in the app.
 * Supports different types of feedback for various game events.
 */
class HapticFeedbackManager(private val context: Context) {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Light tap feedback for placing symbols
     */
    fun tap() {
        vibrate(10L, VibrationEffect.EFFECT_TICK)
    }

    /**
     * Medium feedback for important actions (forming a mill, capturing)
     */
    fun success() {
        vibrate(30L, VibrationEffect.EFFECT_CLICK)
    }

    /**
     * Strong feedback for game events (win, loss)
     */
    fun gameEvent() {
        vibrate(50L, VibrationEffect.EFFECT_HEAVY_CLICK)
    }

    /**
     * Error feedback (invalid move)
     */
    fun error() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        } else {
            vibrate(20L)
        }
    }

    /**
     * Celebration pattern for winning
     */
    fun win() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 50, 50, 50, 50, 100)
            val amplitudes = intArrayOf(0, 100, 0, 150, 0, 255)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 50, 50, 50, 50, 100), -1)
        }
    }

    /**
     * Sad pattern for losing
     */
    fun lose() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(200, 100))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(200)
        }
    }

    /**
     * Bomb explosion feedback
     */
    fun explosion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 100, 30, 50)
            val amplitudes = intArrayOf(0, 255, 0, 150)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 100, 30, 50), -1)
        }
    }

    private fun vibrate(durationMs: Long, effectId: Int? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && effectId != null) {
            vibrator?.vibrate(VibrationEffect.createPredefined(effectId))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(durationMs)
        }
    }

    companion object {
        @Volatile
        private var instance: HapticFeedbackManager? = null

        fun getInstance(context: Context): HapticFeedbackManager {
            return instance ?: synchronized(this) {
                instance ?: HapticFeedbackManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

/**
 * Composable to get HapticFeedbackManager
 */
@Composable
fun rememberHapticFeedback(): HapticFeedbackManager {
    val context = LocalContext.current
    return remember { HapticFeedbackManager.getInstance(context) }
}

/**
 * Extension function for View-based haptic feedback (for Compose)
 */
fun View.performHapticFeedback(type: Int = HapticFeedbackConstants.KEYBOARD_TAP) {
    performHapticFeedback(type, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
}
