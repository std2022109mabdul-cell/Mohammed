package com.example

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.ui.theme.ThemeColorConfig
import com.example.ui.viewmodel.AppointmentViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request runtime permission for Post Notifications on Android 13+ (API 33+)
        requestNotificationPermission()
        
        // Request Exact Alarm permission on Android 12+ if not granted
        requestExactAlarmPermission()

        // Initialize local persistence database
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = AppointmentRepository(database.appointmentDao())

        // Initialize ViewModel using Factory
        val viewModel: AppointmentViewModel by viewModels {
            AppointmentViewModel.Factory(application, repository)
        }

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        setContent {
            // Theme override states: null = auto system dark theme, false = light, true = dark
            val storedDarkThemeValue = prefs.getString("dark_theme", "auto")
            var darkThemeOverride by remember { 
                mutableStateOf(
                    when (storedDarkThemeValue) {
                        "dark" -> true
                        "light" -> false
                        else -> null
                    }
                ) 
            }
            
            val storedThemeColorStr = prefs.getString("theme_color", ThemeColorConfig.TEAL.name)
            var themeColor by remember { 
                mutableStateOf(ThemeColorConfig.valueOf(storedThemeColorStr ?: ThemeColorConfig.TEAL.name)) 
            }

            val systemInDark = isSystemInDarkTheme()
            val darkTheme = when (darkThemeOverride) {
                true -> true
                false -> false
                else -> systemInDark
            }

            MyApplicationTheme(darkTheme = darkTheme, themeColor = themeColor) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(
                        viewModel = viewModel,
                        currentThemeColor = themeColor,
                        onThemeColorChange = { newColor -> 
                            themeColor = newColor
                            prefs.edit().putString("theme_color", newColor.name).apply()
                        },
                        darkThemeOverride = darkThemeOverride,
                        onThemeToggle = { newOverride -> 
                            darkThemeOverride = newOverride
                            val valToStore = when (newOverride) {
                                true -> "dark"
                                false -> "light"
                                else -> "auto"
                            }
                            prefs.edit().putString("dark_theme", valToStore).apply()
                        }
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

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
}
