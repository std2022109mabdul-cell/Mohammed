package com.example.data

import kotlinx.coroutines.flow.Flow

class AppointmentRepository(private val appointmentDao: AppointmentDao) {
    val allAppointments: Flow<List<Appointment>> = appointmentDao.getAllAppointments()

    suspend fun getAppointmentById(id: Int): Appointment? {
        return appointmentDao.getAppointmentById(id)
    }

    suspend fun insert(appointment: Appointment): Long {
        return appointmentDao.insertAppointment(appointment)
    }

    suspend fun update(appointment: Appointment) {
        appointmentDao.updateAppointment(appointment)
    }

    suspend fun delete(id: Int) {
        appointmentDao.deleteAppointmentById(id)
    }

    suspend fun getUpcomingUntriggered(currentMillis: Long): List<Appointment> {
        return appointmentDao.getUpcomingUntriggeredAppointments(currentMillis)
    }
}
