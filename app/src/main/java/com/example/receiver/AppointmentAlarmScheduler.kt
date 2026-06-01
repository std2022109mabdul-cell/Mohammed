package com.example.receiver

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.Appointment

class AppointmentAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @SuppressLint("ScheduleExactAlarm")
    fun schedule(appointment: Appointment) {
        val triggerTime = appointment.epochMillis - (appointment.alertOffsetMinutes * 60_000L)
        val now = System.currentTimeMillis()
        
        // If it passed more than 5 minutes ago, skip it
        if (triggerTime < now - (5 * 60_000L)) {
            Log.d("AlarmScheduler", "Trigger time past by >5 mins for ${appointment.id}. Skipping.")
            return
        }
        
        // If it just passed, trigger immediately
        val finalTriggerTime = if (triggerTime < now) now + 1000L else triggerTime

        val intent = Intent(context, AppointmentReminderReceiver::class.java).apply {
            putExtra("EXTRA_APPOINTMENT_ID", appointment.id)
            putExtra("EXTRA_APPOINTMENT_TITLE", appointment.title)
            putExtra("EXTRA_APPOINTMENT_DESC", appointment.description)
            putExtra("EXTRA_ALERT_TYPE", appointment.alertType)
            putExtra("EXTRA_SOUND_TONE", appointment.soundTone)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appointment.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        finalTriggerTime,
                        pendingIntent
                    )
                    Log.d("AlarmScheduler", "Scheduled EXACT alarm for ${appointment.title} at $finalTriggerTime")
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        finalTriggerTime,
                        pendingIntent
                    )
                    Log.d("AlarmScheduler", "Scheduled inexact fallback alarm for ${appointment.title} (No permission)")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    finalTriggerTime,
                    pendingIntent
                )
                Log.d("AlarmScheduler", "Scheduled exact alarm for ${appointment.title} (Pre-S)")
            }
        } catch (e: SecurityException) {
            // Fallback for security exception (if system rejects exact alarm scheduling)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                finalTriggerTime,
                pendingIntent
            )
            Log.e("AlarmScheduler", "SecurityException scheduling exact alarm. Fell back to inexact.", e)
        }
    }

    fun cancel(appointment: Appointment) {
        val intent = Intent(context, AppointmentReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appointment.id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("AlarmScheduler", "Canceled alarm for appointment ${appointment.id}")
        }
    }
}
