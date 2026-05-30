package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val pendingResult = goAsync()
            val scheduler = AppointmentAlarmScheduler(context)
            val db = AppDatabase.getDatabase(context)
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val now = System.currentTimeMillis()
                    val appointments = db.appointmentDao().getUpcomingUntriggeredAppointments(0)
                    
                    appointments.forEach { appointment ->
                        if (!appointment.isCompleted && !appointment.isTriggered) {
                            val triggerTime = appointment.epochMillis - (appointment.alertOffsetMinutes * 60 * 1000)
                            if (triggerTime > now) {
                                scheduler.schedule(appointment)
                            }
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
