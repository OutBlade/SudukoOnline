package de.sudokuonline.app.util

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Music Manager for ambient background music
 * Generates beautiful procedural ambient music with proper musical theory
 */
class MusicManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    private var audioTrack: AudioTrack? = null
    private var musicJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isEnabled = MutableStateFlow(prefs.getBoolean(KEY_MUSIC_ENABLED, false))
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _volume = MutableStateFlow(prefs.getFloat(KEY_VOLUME, 0.5f))
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _currentTrack = MutableStateFlow(prefs.getInt(KEY_TRACK, 0))
    val currentTrack: StateFlow<Int> = _currentTrack.asStateFlow()

    // Available ambient tracks
    val tracks = listOf(
        TrackInfo("Peaceful Dreams", "Sanfte Klaviermelodie mit Pad"),
        TrackInfo("Zen Garden", "Japanische Pentatonik"),
        TrackInfo("Cosmic Journey", "Atmosphärische Synthesizer"),
        TrackInfo("Rain Cafe", "Lo-Fi Chill Beats"),
        TrackInfo("Morning Dew", "Sanfte Harfen-Arpeggios")
    )

    init {
        if (_isEnabled.value) {
            startMusic()
        }
    }

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MUSIC_ENABLED, enabled).apply()
        _isEnabled.value = enabled
        if (enabled) {
            startMusic()
        } else {
            stopMusic()
        }
    }

    fun setVolume(vol: Float) {
        val clampedVol = vol.coerceIn(0f, 1f)
        prefs.edit().putFloat(KEY_VOLUME, clampedVol).apply()
        _volume.value = clampedVol
        audioTrack?.setVolume(clampedVol)
    }

    fun setTrack(trackIndex: Int) {
        if (trackIndex in tracks.indices) {
            prefs.edit().putInt(KEY_TRACK, trackIndex).apply()
            _currentTrack.value = trackIndex
            if (_isPlaying.value) {
                stopMusic()
                startMusic()
            }
        }
    }

    fun startMusic() {
        if (_isPlaying.value) return

        musicJob = scope.launch {
            _isPlaying.value = true
            playGenerativeMusic()
        }
    }

    fun stopMusic() {
        musicJob?.cancel()
        musicJob = null
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        _isPlaying.value = false
    }

    fun toggleMusic() {
        if (_isPlaying.value) {
            stopMusic()
            setEnabled(false)
        } else {
            setEnabled(true)
        }
    }

    private suspend fun playGenerativeMusic() {
        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 4

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.setVolume(_volume.value)
        audioTrack?.play()

        val composer = when (_currentTrack.value) {
            0 -> PeaceflDreamsComposer(sampleRate)
            1 -> ZenGardenComposer(sampleRate)
            2 -> CosmicJourneyComposer(sampleRate)
            3 -> RainCafeComposer(sampleRate)
            4 -> MorningDewComposer(sampleRate)
            else -> PeaceflDreamsComposer(sampleRate)
        }

        val buffer = ShortArray(bufferSize / 2)

        while (coroutineContext.isActive && _isPlaying.value) {
            for (i in buffer.indices step 2) {
                val sample = composer.nextSample()
                val left = (sample.first * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                val right = (sample.second * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                buffer[i] = left
                buffer[i + 1] = right
            }

            audioTrack?.write(buffer, 0, buffer.size)
            yield()
        }
    }

    fun release() {
        stopMusic()
        scope.cancel()
    }

    companion object {
        private const val PREFS_NAME = "music_settings"
        private const val KEY_MUSIC_ENABLED = "music_enabled"
        private const val KEY_VOLUME = "music_volume"
        private const val KEY_TRACK = "music_track"

        @Volatile
        private var instance: MusicManager? = null

        fun getInstance(context: Context): MusicManager {
            return instance ?: synchronized(this) {
                instance ?: MusicManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

data class TrackInfo(
    val name: String,
    val description: String
)

/**
 * Base class for music composers
 */
abstract class MusicComposer(protected val sampleRate: Int) {
    protected var sampleIndex = 0L
    protected val random = Random(System.currentTimeMillis())

    abstract fun nextSample(): Pair<Double, Double>

    // Convert MIDI note to frequency
    protected fun midiToFreq(note: Int): Double = 440.0 * Math.pow(2.0, (note - 69) / 12.0)

    // Soft sine wave
    protected fun sine(phase: Double): Double = sin(2 * PI * phase)

    // Soft triangle wave
    protected fun triangle(phase: Double): Double {
        val p = phase % 1.0
        return if (p < 0.5) 4 * p - 1 else 3 - 4 * p
    }

    // Soft saw wave (band-limited approximation)
    protected fun softSaw(phase: Double): Double {
        var sum = 0.0
        for (k in 1..8) {
            sum += sin(2 * PI * k * phase) / k
        }
        return sum * 0.5
    }

    // ADSR envelope
    protected fun adsr(t: Double, attack: Double, decay: Double, sustain: Double, release: Double, duration: Double): Double {
        return when {
            t < attack -> t / attack
            t < attack + decay -> 1.0 - (1.0 - sustain) * (t - attack) / decay
            t < duration - release -> sustain
            t < duration -> sustain * (duration - t) / release
            else -> 0.0
        }
    }

    // Low-pass filter simulation
    protected fun lowPass(input: Double, prev: Double, cutoff: Double): Double {
        val rc = 1.0 / (2 * PI * cutoff)
        val dt = 1.0 / sampleRate
        val alpha = dt / (rc + dt)
        return prev + alpha * (input - prev)
    }

    // Reverb simulation (simple delay-based)
    protected class SimpleReverb(val size: Int, val decay: Float = 0.3f) {
        private val buffer = DoubleArray(size)
        private var index = 0

        fun process(input: Double): Double {
            val delayed = buffer[index]
            buffer[index] = input + delayed * decay
            index = (index + 1) % size
            return input + delayed * 0.5
        }
    }
}

/**
 * Peaceful Dreams - Soft piano with ambient pad
 * Key: C Major, 60 BPM, 4/4 time
 */
class PeaceflDreamsComposer(sampleRate: Int) : MusicComposer(sampleRate) {
    private val bpm = 60.0
    private val samplesPerBeat = (sampleRate * 60.0 / bpm).toInt()

    // C Major pentatonic: C, D, E, G, A (MIDI notes)
    private val scale = listOf(60, 62, 64, 67, 69, 72, 74, 76, 79, 81)

    // Chord progression: C - Am - F - G (I - vi - IV - V)
    private val chords = listOf(
        listOf(48, 52, 55),  // C major
        listOf(45, 48, 52),  // A minor
        listOf(41, 45, 48),  // F major
        listOf(43, 47, 50)   // G major
    )

    private var currentChord = 0
    private var melodyNote = 0
    private var nextMelodyTime = 0L
    private var melodyPhase = 0.0
    private var padPhases = DoubleArray(3)
    private var bassPhase = 0.0
    private var filterState = 0.0

    private val reverb = SimpleReverb(sampleRate / 4, 0.25f)

    override fun nextSample(): Pair<Double, Double> {
        val beatPosition = (sampleIndex % (samplesPerBeat * 16)).toDouble() / samplesPerBeat
        val measurePosition = beatPosition / 4.0

        // Change chord every measure
        currentChord = (measurePosition.toInt()) % chords.size

        var output = 0.0

        // Ambient Pad (soft synth chords)
        val chord = chords[currentChord]
        for (i in chord.indices) {
            val freq = midiToFreq(chord[i] + 12) // One octave up
            padPhases[i] += freq / sampleRate
            val padEnv = 0.15 * (1.0 + 0.1 * sine(sampleIndex.toDouble() / sampleRate * 0.2))
            output += sine(padPhases[i]) * padEnv
            output += sine(padPhases[i] * 2.001) * padEnv * 0.3 // Slight detune for richness
        }

        // Bass note (root of chord)
        val bassFreq = midiToFreq(chord[0] - 12)
        bassPhase += bassFreq / sampleRate
        val bassEnv = 0.2 * adsr(beatPosition % 4, 0.1, 0.3, 0.6, 0.5, 4.0)
        output += triangle(bassPhase) * bassEnv

        // Melody (piano-like)
        if (sampleIndex >= nextMelodyTime) {
            // Pick a note from scale that fits the chord
            val chordTones = chord.map { it % 12 }
            val goodNotes = scale.filter { (it % 12) in chordTones || random.nextFloat() > 0.6 }
            melodyNote = goodNotes[random.nextInt(goodNotes.size)]

            // Random rhythm: quarter, half, or whole note
            val durations = listOf(1, 2, 2, 4)
            nextMelodyTime = sampleIndex + samplesPerBeat * durations[random.nextInt(durations.size)]
            melodyPhase = 0.0
        }

        val melodyFreq = midiToFreq(melodyNote)
        melodyPhase += melodyFreq / sampleRate
        val timeSinceNote = (sampleIndex - (nextMelodyTime - samplesPerBeat * 2)).toDouble() / sampleRate
        val melodyEnv = 0.25 * adsr(timeSinceNote.coerceAtLeast(0.0), 0.01, 0.2, 0.4, 0.8, 3.0)

        // Piano-like timbre (fundamental + harmonics with decay)
        val piano = sine(melodyPhase) * 1.0 +
                sine(melodyPhase * 2) * 0.5 * exp(-timeSinceNote * 2) +
                sine(melodyPhase * 3) * 0.25 * exp(-timeSinceNote * 3) +
                sine(melodyPhase * 4) * 0.125 * exp(-timeSinceNote * 4)
        output += piano * melodyEnv

        // Apply filter and reverb
        filterState = lowPass(output, filterState, 2000.0)
        output = reverb.process(filterState)

        sampleIndex++

        // Stereo spread
        val pan = 0.1 * sine(sampleIndex.toDouble() / sampleRate * 0.1)
        return Pair(output * (0.5 - pan) * 0.7, output * (0.5 + pan) * 0.7)
    }
}

/**
 * Zen Garden - Japanese pentatonic scale with koto-like sounds
 * Key: Japanese In scale, 55 BPM
 */
class ZenGardenComposer(sampleRate: Int) : MusicComposer(sampleRate) {
    private val bpm = 55.0
    private val samplesPerBeat = (sampleRate * 60.0 / bpm).toInt()

    // Japanese In scale (minor pentatonic variant): E, F, A, B, C
    private val scale = listOf(52, 53, 57, 59, 60, 64, 65, 69, 71, 72)

    private var melodyNote = scale[0]
    private var nextNoteTime = 0L
    private var notePhase = 0.0
    private var noteStartTime = 0L
    private var dronePhase = 0.0
    private var filterState = 0.0

    private val reverb = SimpleReverb(sampleRate / 3, 0.35f)

    override fun nextSample(): Pair<Double, Double> {
        var output = 0.0

        // Drone (low E with fifth)
        val droneFreq = midiToFreq(40) // Low E
        dronePhase += droneFreq / sampleRate
        output += sine(dronePhase) * 0.12
        output += sine(dronePhase * 1.5) * 0.06 // Fifth

        // Koto-like melody
        if (sampleIndex >= nextNoteTime) {
            // Tend to move by small intervals
            val currentIndex = scale.indexOf(melodyNote).coerceAtLeast(0)
            val movement = listOf(-2, -1, -1, 0, 1, 1, 2)[random.nextInt(7)]
            val newIndex = (currentIndex + movement).coerceIn(0, scale.size - 1)
            melodyNote = scale[newIndex]
            noteStartTime = sampleIndex

            // Varied rhythm with rests
            val durations = listOf(1, 1, 2, 2, 3, 4, 6)
            val duration = durations[random.nextInt(durations.size)]
            nextNoteTime = sampleIndex + samplesPerBeat * duration

            // Occasional rest
            if (random.nextFloat() < 0.2) {
                nextNoteTime += samplesPerBeat * 2
            }
        }

        val timeSinceNote = (sampleIndex - noteStartTime).toDouble() / sampleRate
        val noteFreq = midiToFreq(melodyNote)
        notePhase += noteFreq / sampleRate

        // Koto timbre: plucked string with quick attack and long decay
        val kotoEnv = 0.35 * exp(-timeSinceNote * 1.5) * (1 - exp(-timeSinceNote * 50))
        val koto = sine(notePhase) +
                0.6 * sine(notePhase * 2) * exp(-timeSinceNote * 3) +
                0.3 * sine(notePhase * 3) * exp(-timeSinceNote * 5) +
                0.15 * sine(notePhase * 4) * exp(-timeSinceNote * 7)
        output += koto * kotoEnv

        // Occasional high harmonic (like touching the string)
        if (timeSinceNote > 0.5 && timeSinceNote < 0.55) {
            output += sine(notePhase * 4) * 0.1
        }

        // Filter and reverb
        filterState = lowPass(output, filterState, 3000.0)
        output = reverb.process(filterState)

        sampleIndex++

        // Wide stereo for spaciousness
        val pan = 0.2 * sine(sampleIndex.toDouble() / sampleRate * 0.05)
        return Pair(output * (0.5 - pan) * 0.7, output * (0.5 + pan) * 0.7)
    }
}

/**
 * Cosmic Journey - Atmospheric synthesizer pads
 * Key: D minor, 50 BPM, evolving textures
 */
class CosmicJourneyComposer(sampleRate: Int) : MusicComposer(sampleRate) {
    private val bpm = 50.0
    private val samplesPerBeat = (sampleRate * 60.0 / bpm).toInt()

    // D minor scale
    private val chords = listOf(
        listOf(38, 45, 50, 57),  // Dm7
        listOf(36, 43, 48, 55),  // C major 7
        listOf(41, 48, 53, 60),  // F major 7
        listOf(38, 45, 50, 57)   // Dm7
    )

    private var currentChord = 0
    private var padPhases = DoubleArray(4)
    private var lfoPhase = 0.0
    private var sparklePhase = 0.0
    private var sparkleNote = 74
    private var nextSparkleTime = 0L
    private var filterState = 0.0
    private var filterState2 = 0.0

    private val reverb = SimpleReverb(sampleRate / 2, 0.4f)

    override fun nextSample(): Pair<Double, Double> {
        val time = sampleIndex.toDouble() / sampleRate
        val measurePosition = (sampleIndex % (samplesPerBeat * 16)).toDouble() / (samplesPerBeat * 4)

        // Slow chord changes
        currentChord = (measurePosition.toInt()) % chords.size

        var output = 0.0

        // LFO for movement
        lfoPhase += 0.1 / sampleRate
        val lfo = sine(lfoPhase)
        val lfo2 = sine(lfoPhase * 1.7)

        // Rich pad with detuned oscillators
        val chord = chords[currentChord]
        for (i in chord.indices) {
            val baseFreq = midiToFreq(chord[i])
            padPhases[i] += baseFreq / sampleRate

            // Multiple detuned oscillators per note
            val osc1 = sine(padPhases[i])
            val osc2 = sine(padPhases[i] * 1.002 + lfo * 0.01)
            val osc3 = sine(padPhases[i] * 0.998 + lfo2 * 0.01)
            val osc4 = triangle(padPhases[i] * 0.5) * 0.3

            output += (osc1 + osc2 + osc3 + osc4) * 0.08
        }

        // Sparkle/bells on top
        if (sampleIndex >= nextSparkleTime) {
            val highNotes = listOf(74, 77, 81, 84, 86, 89)
            sparkleNote = highNotes[random.nextInt(highNotes.size)]
            nextSparkleTime = sampleIndex + samplesPerBeat * (2 + random.nextInt(4))
            sparklePhase = 0.0
        }

        val sparkleTime = (sampleIndex - (nextSparkleTime - samplesPerBeat * 3)).toDouble() / sampleRate
        if (sparkleTime > 0) {
            sparklePhase += midiToFreq(sparkleNote) / sampleRate
            val sparkleEnv = 0.15 * exp(-sparkleTime * 2) * (1 - exp(-sparkleTime * 30))
            output += sine(sparklePhase) * sparkleEnv
            output += sine(sparklePhase * 2) * sparkleEnv * 0.3
        }

        // Dual filter for movement
        filterState = lowPass(output, filterState, 800 + 600 * (1 + lfo))
        filterState2 = lowPass(filterState, filterState2, 2000.0)
        output = reverb.process(filterState2)

        sampleIndex++

        // Slow stereo movement
        val pan = 0.15 * sine(time * 0.03)
        return Pair(output * (0.5 - pan) * 0.75, output * (0.5 + pan) * 0.75)
    }
}

/**
 * Rain Cafe - Lo-Fi Chill Beats
 * Key: F Major, 75 BPM with swing
 */
class RainCafeComposer(sampleRate: Int) : MusicComposer(sampleRate) {
    private val bpm = 75.0
    private val samplesPerBeat = (sampleRate * 60.0 / bpm).toInt()

    // F Major 7 and related chords
    private val chords = listOf(
        listOf(41, 48, 52, 55),  // Fmaj7
        listOf(38, 45, 50, 53),  // Dm7
        listOf(43, 50, 55, 59),  // Gm7
        listOf(36, 43, 48, 52)   // C7
    )

    private var currentChord = 0
    private var padPhases = DoubleArray(4)
    private var bassPhase = 0.0
    private var hihatPhase = 0.0
    private var kickPhase = 0.0
    private var filterState = 0.0

    private val reverb = SimpleReverb(sampleRate / 5, 0.2f)

    override fun nextSample(): Pair<Double, Double> {
        val beatPosition = (sampleIndex % (samplesPerBeat * 8)).toDouble() / samplesPerBeat
        val measurePosition = beatPosition / 4.0

        currentChord = (measurePosition.toInt()) % chords.size
        val chord = chords[currentChord]

        var output = 0.0

        // Lo-fi electric piano
        for (i in chord.indices) {
            val freq = midiToFreq(chord[i] + 12)
            padPhases[i] += freq / sampleRate

            val epEnv = 0.12 * adsr(beatPosition % 2, 0.02, 0.3, 0.5, 0.5, 2.0)
            val ep = sine(padPhases[i]) +
                    0.5 * sine(padPhases[i] * 2) +
                    0.2 * sine(padPhases[i] * 3) * exp(-beatPosition % 2)
            output += ep * epEnv
        }

        // Simple bass
        val bassFreq = midiToFreq(chord[0] - 12)
        bassPhase += bassFreq / sampleRate
        val bassEnv = 0.2 * adsr(beatPosition % 1, 0.01, 0.1, 0.7, 0.2, 1.0)
        output += (sine(bassPhase) + 0.3 * sine(bassPhase * 2)) * bassEnv

        // Drum pattern (simplified)
        val sixteenth = ((beatPosition * 4) % 16).toInt()

        // Kick on 1 and 3 (with slight swing)
        if (sixteenth == 0 || sixteenth == 8) {
            kickPhase = 0.0
        }
        kickPhase += (60 - kickPhase * 50).coerceAtLeast(30.0) / sampleRate
        val kickEnv = exp(-kickPhase * 15) * 0.3
        output += sine(kickPhase * 2 * PI * 2) * kickEnv

        // Hi-hat pattern (lo-fi, quiet)
        if (sixteenth % 2 == 0) {
            hihatPhase = random.nextDouble() * 0.1
        }
        hihatPhase += 8000.0 / sampleRate
        val hihatEnv = exp(-(beatPosition * 4 % 1) * 20) * 0.05
        output += (random.nextDouble() * 2 - 1) * hihatEnv

        // Lo-fi effect: bit crush simulation and filter
        output = (output * 16).toInt() / 16.0  // Subtle bit reduction
        filterState = lowPass(output, filterState, 4000.0)
        output = reverb.process(filterState)

        sampleIndex++

        // Slight stereo wobble
        val pan = 0.05 * sine(sampleIndex.toDouble() / sampleRate * 0.5)
        return Pair(output * (0.5 - pan) * 0.7, output * (0.5 + pan) * 0.7)
    }
}

/**
 * Morning Dew - Gentle harp arpeggios
 * Key: G Major, 65 BPM
 */
class MorningDewComposer(sampleRate: Int) : MusicComposer(sampleRate) {
    private val bpm = 65.0
    private val samplesPerBeat = (sampleRate * 60.0 / bpm).toInt()

    // G Major arpeggios
    private val arpeggios = listOf(
        listOf(55, 59, 62, 67, 71, 74),  // G major
        listOf(52, 55, 59, 64, 67, 71),  // Em
        listOf(48, 52, 55, 60, 64, 67),  // C major
        listOf(50, 54, 57, 62, 66, 69)   // D major
    )

    private var currentArp = 0
    private var arpIndex = 0
    private var notePhase = 0.0
    private var noteStartTime = 0L
    private var currentNote = 55
    private var nextNoteTime = 0L
    private var padPhase = 0.0
    private var filterState = 0.0

    private val reverb = SimpleReverb(sampleRate / 3, 0.35f)

    override fun nextSample(): Pair<Double, Double> {
        val beatPosition = (sampleIndex % (samplesPerBeat * 16)).toDouble() / samplesPerBeat
        val measurePosition = (beatPosition / 4.0).toInt()

        currentArp = measurePosition % arpeggios.size
        val arp = arpeggios[currentArp]

        var output = 0.0

        // Soft pad in background
        val padFreq = midiToFreq(arp[0] - 12)
        padPhase += padFreq / sampleRate
        output += sine(padPhase) * 0.08
        output += sine(padPhase * 1.5) * 0.04 // Fifth

        // Arpeggio
        val arpSpeed = samplesPerBeat / 3  // Triplet feel
        if (sampleIndex >= nextNoteTime) {
            currentNote = arp[arpIndex % arp.size]
            arpIndex++

            // Go up then down
            if (arpIndex >= arp.size * 2) {
                arpIndex = 0
            }
            val actualIndex = if (arpIndex < arp.size) arpIndex else arp.size * 2 - 1 - arpIndex

            currentNote = arp[actualIndex.coerceIn(0, arp.size - 1)]
            noteStartTime = sampleIndex
            nextNoteTime = sampleIndex + arpSpeed
            notePhase = 0.0
        }

        val timeSinceNote = (sampleIndex - noteStartTime).toDouble() / sampleRate
        val noteFreq = midiToFreq(currentNote)
        notePhase += noteFreq / sampleRate

        // Harp-like timbre
        val harpEnv = 0.3 * exp(-timeSinceNote * 2.5) * (1 - exp(-timeSinceNote * 80))
        val harp = sine(notePhase) +
                0.4 * sine(notePhase * 2) * exp(-timeSinceNote * 4) +
                0.2 * sine(notePhase * 3) * exp(-timeSinceNote * 6) +
                0.1 * sine(notePhase * 4) * exp(-timeSinceNote * 8)
        output += harp * harpEnv

        // Gentle filter and reverb
        filterState = lowPass(output, filterState, 5000.0)
        output = reverb.process(filterState)

        sampleIndex++

        // Stereo spread based on note pitch
        val pan = (currentNote - 60) * 0.015
        return Pair(output * (0.5 - pan) * 0.65, output * (0.5 + pan) * 0.65)
    }
}

/**
 * Composable to get MusicManager
 */
@Composable
fun rememberMusicManager(): MusicManager {
    val context = LocalContext.current
    return remember { MusicManager.getInstance(context) }
}
