package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class AppointmentReminderReceiver : BroadcastReceiver() {

    private var tts: TextToSpeech? = null

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        
        val id = intent.getIntExtra("EXTRA_APPOINTMENT_ID", -1)
        val title = intent.getStringExtra("EXTRA_APPOINTMENT_TITLE") ?: "موعد"
        val desc = intent.getStringExtra("EXTRA_APPOINTMENT_DESC") ?: ""
        val alertType = intent.getStringExtra("EXTRA_ALERT_TYPE") ?: "VOICE"
        val soundTone = intent.getStringExtra("EXTRA_SOUND_TONE") ?: "DEFAULT"

        Log.d("ReminderReceiver", "Alarm received for ID $id, Title: $title, Alert: $alertType, Tone: $soundTone")

        // Trigger Notification
        showNotification(context, id, title, desc, alertType, soundTone)

        // Mark the appointment as triggered in database
        val dbUpdateJob = CoroutineScope(Dispatchers.IO).launch {
            if (id != -1) {
                val db = AppDatabase.getDatabase(context)
                val dao = db.appointmentDao()
                val appointment = dao.getAppointmentById(id)
                if (appointment != null) {
                    dao.updateAppointment(appointment.copy(isTriggered = true, isCompleted = true))
                    Log.d("ReminderReceiver", "Marked appointment $id as triggered & completed")
                }
            }
        }

        handleVibrationAndVoice(context, alertType, soundTone, title) {
            dbUpdateJob.invokeOnCompletion {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        id: Int,
        title: String,
        desc: String,
        alertType: String,
        soundTone: String
    ) {
        val channelId = "appointment_reminders_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "تنبيهات المواعيد",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "قناة لتنبيه المواعيد الهامة"
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Open app on click
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("تذكير بموعدك: $title")
            .setContentText(desc.ifEmpty { "لديك موعد مجدول الآن" })
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true) // High priority heads-up
            .setAutoCancel(true)

        notificationManager.notify(id, notificationBuilder.build())
    }

    private fun handleVibrationAndVoice(
        context: Context, 
        alertType: String, 
        soundTone: String, 
        title: String,
        onComplete: () -> Unit
    ) {
        if (alertType == "SILENT") {
            onComplete()
            return
        }

        // 1. Vibrator Trigger
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 400, 200, 400, 200, 400),
                        -1 // No repeat
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 400, 200, 400, 200, 400), -1)
            }
        } catch (e: Exception) {
            Log.e("ReminderReceiver", "Failed to vibrate device", e)
        }

        // 2. Sound or TTS
        if (alertType == "TTS") {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale("ar"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.setLanguage(Locale.US)
                    }
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) {
                            onComplete()
                            tts?.shutdown()
                        }
                        override fun onError(utteranceId: String?) {
                            onComplete()
                            tts?.shutdown()
                        }
                    })
                    val speechText = "تذكير بموعدك الآن: $title"
                    tts?.speak(speechText, TextToSpeech.QUEUE_ADD, null, "AppointmentTTS")
                    Log.d("ReminderReceiver", "Spoken reminder successfully")
                } else {
                    onComplete()
                }
            }
        } else if (alertType == "VOICE") {
            when (soundTone) {
                "DEFAULT" -> playDefaultRingtone(context, onComplete)
                "DIGITAL_BEEP" -> {
                    playDigitalBeep()
                    CoroutineScope(Dispatchers.Default).launch {
                        kotlinx.coroutines.delay(2000)
                        onComplete()
                    }
                }
                "SOFT_BELL" -> {
                    playSoftBell()
                    CoroutineScope(Dispatchers.Default).launch {
                        kotlinx.coroutines.delay(2000)
                        onComplete()
                    }
                }
                "CHIPTUNE" -> playSynthesizedChiptune(onComplete)
                else -> {
                    playDefaultRingtone(context, onComplete)
                }
            }
        } else if (alertType == "VIBRATE") {
            CoroutineScope(Dispatchers.Default).launch {
                kotlinx.coroutines.delay(2000)
                onComplete()
            }
        } else {
            onComplete()
        }
    }

    private fun playDefaultRingtone(context: Context, onComplete: () -> Unit) {
        try {
            val alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val ringtone = RingtoneManager.getRingtone(context, alert)
            ringtone.play()
            // Stop playing after 6 seconds
            CoroutineScope(Dispatchers.Default).launch {
                kotlinx.coroutines.delay(6000)
                if (ringtone.isPlaying) {
                    ringtone.stop()
                }
                onComplete()
            }
        } catch (e: Exception) {
            Log.e("ReminderReceiver", "Error playing default ringtone", e)
            onComplete()
        }
    }

    private fun playDigitalBeep() {
        try {
            val toneG = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1500)
        } catch (e: Exception) {
            Log.e("ReminderReceiver", "Error playing digital beep", e)
        }
    }

    private fun playSoftBell() {
        try {
            val toneG = ToneGenerator(AudioManager.STREAM_ALARM, 80)
            // Play a gentle two-tone sequence
            CoroutineScope(Dispatchers.IO).launch {
                toneG.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
                kotlinx.coroutines.delay(500)
                toneG.startTone(ToneGenerator.TONE_PROP_PROMPT, 400)
            }
        } catch (e: Exception) {
            Log.e("ReminderReceiver", "Error playing soft bell", e)
        }
    }

    private fun playSynthesizedChiptune(onComplete: () -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val sampleRate = 8000
                val sampleNotes = doubleArrayOf(523.25, 587.33, 659.25, 783.99, 1046.50) // C, D, E, G, C
                val durationStep = 0.25 // seconds per note
                val bytesPerNote = (durationStep * sampleRate * 2).toInt() // 16-bit sound
                val soundBytes = ByteArray(bytesPerNote * sampleNotes.size)

                for (noteIdx in sampleNotes.indices) {
                    val freq = sampleNotes[noteIdx]
                    val startIndex = noteIdx * bytesPerNote
                    for (i in 0 until (durationStep * sampleRate).toInt()) {
                        // Square wave pattern
                        val value = if (Math.sin(2.0 * Math.PI * i * freq / sampleRate) >= 0.0) 12000 else -12000
                        val byteIdx = startIndex + i * 2
                        soundBytes[byteIdx] = (value and 0xFF).toByte()
                        soundBytes[byteIdx + 1] = ((value ushr 8) and 0xFF).toByte()
                    }
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
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

                audioTrack.write(soundBytes, 0, soundBytes.size)
                audioTrack.play()
                
                // Release audio track resources after playback
                kotlinx.coroutines.delay(2000)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Error generating synth melody", e)
            } finally {
                onComplete()
            }
        }
    }
}
