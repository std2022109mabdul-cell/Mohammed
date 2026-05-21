package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Appointment
import com.example.data.AppointmentRepository
import com.example.receiver.AppointmentAlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppointmentViewModel(
    application: Application,
    private val repository: AppointmentRepository
) : AndroidViewModel(application) {

    private val alarmScheduler = AppointmentAlarmScheduler(application)

    // Reactive list of all appointments from local database
    val allAppointments: StateFlow<List<Appointment>> = repository.allAppointments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filter appointments into active/past with reactive search integration
    val filteredAppointments: StateFlow<FilteredState> = combine(
        allAppointments,
        searchQuery
    ) { list, query ->
        val filteredList = if (query.isBlank()) {
            list
        } else {
            list.filter {
                it.title.contains(query, ignoreCase = true) || 
                it.description.contains(query, ignoreCase = true)
            }
        }

        val now = System.currentTimeMillis()
        val upcoming = filteredList.filter { !it.isCompleted && it.epochMillis > now }
        val past = filteredList.filter { it.isCompleted || it.epochMillis <= now }

        FilteredState(upcoming = upcoming, past = past)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FilteredState()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addAppointment(appointment: Appointment) {
        viewModelScope.launch {
            try {
                val newId = repository.insert(appointment)
                val appointmentWithId = appointment.copy(id = newId.toInt())
                Log.d("AppointmentViewModel", "Inserted appointment: $appointmentWithId")
                
                // Schedule alarm in Android system
                alarmScheduler.schedule(appointmentWithId)
            } catch (e: Exception) {
                Log.e("AppointmentViewModel", "Error adding appointment", e)
            }
        }
    }

    fun updateAppointment(appointment: Appointment) {
        viewModelScope.launch {
            try {
                repository.update(appointment)
                Log.d("AppointmentViewModel", "Updated appointment: ${appointment.id}")
                
                // Reschedule alarm (Cancel & schedule new)
                alarmScheduler.cancel(appointment)
                
                val now = System.currentTimeMillis()
                val targetTime = appointment.epochMillis - (appointment.alertOffsetMinutes * 60 * 1000)
                if (!appointment.isCompleted && targetTime > now) {
                    alarmScheduler.schedule(appointment)
                }
            } catch (e: Exception) {
                Log.e("AppointmentViewModel", "Error updating appointment", e)
            }
        }
    }

    fun deleteAppointment(appointment: Appointment) {
        viewModelScope.launch {
            try {
                repository.delete(appointment.id)
                Log.d("AppointmentViewModel", "Deleted appointment: ${appointment.id}")
                
                // Cancel scheduled alert
                alarmScheduler.cancel(appointment)
            } catch (e: Exception) {
                Log.e("AppointmentViewModel", "Error deleting appointment", e)
            }
        }
    }

    fun toggleCompletion(appointment: Appointment) {
        val updated = appointment.copy(isCompleted = !appointment.isCompleted)
        updateAppointment(updated)
    }

    class Factory(
        private val application: Application,
        private val repository: AppointmentRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AppointmentViewModel::class.java)) {
                return AppointmentViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

data class FilteredState(
    val upcoming: List<Appointment> = emptyList(),
    val past: List<Appointment> = emptyList()
)
