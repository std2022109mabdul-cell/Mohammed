package com.example.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class SoundPreviewHelper(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var activeAudioTrack: AudioTrack? = null

    init {
        // Pre-initialize TTS to be ready for instant play
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setLanguage(Locale("ar")) ?: tts?.setLanguage(Locale.US)
            }
        }
    }

    fun playPreview(alertType: String, soundTone: String, title: String) {
        stopPreviousPlayback()

        // 1. Check Vibrator
        if (alertType == "VIBRATE") {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(500)
                }
            } catch (e: Exception) {
                Log.e("SoundPreviewHelper", "Vibration failed", e)
            }
            return
        }

        if (alertType == "SILENT") return

        // 2. Play Audio Alert
        if (alertType == "TTS") {
            val speechText = "تجربة التنبيه المنطوق: $title"
            tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "PreviewTTS")
        } else if (alertType == "VOICE") {
            when (soundTone) {
                "DEFAULT" -> playDefaultRingtone()
                "DIGITAL_BEEP" -> playDigitalBeep()
                "SOFT_BELL" -> playSoftBell()
                "CHIPTUNE" -> playSynthesizedChiptune()
            }
        }
    }

    private fun playDefaultRingtone() {
        try {
            val alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val ringtone = RingtoneManager.getRingtone(context, alert)
            ringtone.play()
            CoroutineScope(Dispatchers.Main).launch {
                kotlinx.coroutines.delay(2000)
                if (ringtone.isPlaying) {
                    ringtone.stop()
                }
            }
        } catch (e: Exception) {
            Log.e("SoundPreviewHelper", "Ringtone preview failed", e)
        }
    }

    private fun playDigitalBeep() {
        try {
            val toneG = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
        } catch (e: Exception) {
            Log.e("SoundPreviewHelper", "Beep preview failed", e)
        }
    }

    private fun playSoftBell() {
        try {
            val toneG = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
            toneG.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
        } catch (e: Exception) {
            Log.e("SoundPreviewHelper", "Bell preview failed", e)
        }
    }

    private fun playSynthesizedChiptune() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val sampleRate = 8000
                val sampleNotes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50) // C, E, G, C2
                val durationStep = 0.2
                val bytesPerNote = (durationStep * sampleRate * 2).toInt()
                val soundBytes = ByteArray(bytesPerNote * sampleNotes.size)

                for (noteIdx in sampleNotes.indices) {
                    val freq = sampleNotes[noteIdx]
                    val startIndex = noteIdx * bytesPerNote
                    for (i in 0 until (durationStep * sampleRate).toInt()) {
                        val value = if (Math.sin(2.0 * Math.PI * i * freq / sampleRate) >= 0) 10000 else -10000
                        val byteIdx = startIndex + i * 2
                        soundBytes[byteIdx] = (value and 0xFF).toByte()
                        soundBytes[byteIdx + 1] = ((value ushr 8) and 0xFF).toByte()
                    }
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(soundBytes.size)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                activeAudioTrack = audioTrack
                audioTrack.write(soundBytes, 0, soundBytes.size)
                audioTrack.play()

                kotlinx.coroutines.delay(1000)
                audioTrack.stop()
                audioTrack.release()
                if (activeAudioTrack == audioTrack) {
                    activeAudioTrack = null
                }
            } catch (e: Exception) {
                Log.e("SoundPreviewHelper", "Synth preview failed", e)
            }
        }
    }

    fun stopPreviousPlayback() {
        try {
            tts?.stop()
            activeAudioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
            activeAudioTrack = null
        } catch (e: Exception) {
            Log.e("SoundPreviewHelper", "Error stopping playback", e)
        }
    }

    fun release() {
        try {
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.e("SoundPreviewHelper", "Shutdown failed", e)
        }
    }
}
