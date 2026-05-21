package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import com.example.data.AppDatabase
import com.example.data.AppointmentRepository
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppointmentViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request runtime permission for Post Notifications on Android 13+ (API 33+)
        requestNotificationPermission()

        // Initialize local persistence database
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = AppointmentRepository(database.appointmentDao())

        // Initialize ViewModel using Factory
        val viewModel: AppointmentViewModel by viewModels {
            AppointmentViewModel.Factory(application, repository)
        }

        setContent {
            // Theme override states: null = auto system dark theme, false = light, true = dark
            var darkThemeOverride by remember { mutableStateOf<Boolean?>(null) }
            val systemInDark = isSystemInDarkTheme()
            val darkTheme = when (darkThemeOverride) {
                true -> true
                false -> false
                else -> systemInDark
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(
                        viewModel = viewModel,
                        darkThemeOverride = darkThemeOverride,
                        onThemeToggle = { darkThemeOverride = it }
                    )
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        }
    }
}
