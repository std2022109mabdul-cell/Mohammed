package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Appointment
import com.example.ui.viewmodel.AppointmentViewModel
import com.example.utils.SoundPreviewHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppointmentViewModel,
    darkThemeOverride: Boolean?,
    onThemeToggle: (Boolean?) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // UI states
    val appointmentsState by viewModel.filteredAppointments.collectAsState()
    val allAppointmentsList by viewModel.allAppointments.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    // Bottom Sheet State
    var showAddEditSheet by remember { mutableStateOf(false) }
    var editingAppointment by remember { mutableStateOf<Appointment?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Mode: "upcoming" or "completed"
    var selectedTab by remember { mutableStateOf("upcoming") }
    
    var globalVoiceData by remember { mutableStateOf("") }
    val globalSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = matches?.firstOrNull() ?: ""
            if (recognizedText.isNotEmpty()) {
                globalVoiceData = recognizedText
                editingAppointment = null
                showAddEditSheet = true
            }
        }
    }

    // Audio preview helper
    val soundPreviewHelper = remember { SoundPreviewHelper(context) }
    DisposableEffect(Unit) {
        onDispose {
            soundPreviewHelper.release()
        }
    }

    // Force RTL local layout direction for Arabic experience
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val totalCount = allAppointmentsList.size
        val upcomingCount = allAppointmentsList.count { !it.isCompleted && it.epochMillis > System.currentTimeMillis() }
        val completedCount = allAppointmentsList.count { it.isCompleted || it.epochMillis <= System.currentTimeMillis() }

        Scaffold(
            topBar = {
                HeaderSection(
                    darkThemeOverride = darkThemeOverride,
                    onThemeToggle = onThemeToggle
                )
            },
            floatingActionButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FloatingActionButton(
                        onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث لتسجيل ملاحظة أو موعد جديد...")
                            }
                            if (intent.resolveActivity(context.packageManager) != null) {
                                try {
                                    globalSpeechLauncher.launch(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "التعرف على الصوت غير مدعوم في جهازك", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "تطبيق التعرف على الصوت غير مثبت بنظامك", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .testTag("dictate_appointment_fab")
                            .padding(bottom = 12.dp)
                            .size(56.dp),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "إملاء موعد بالصوت")
                    }

                    ExtendedFloatingActionButton(
                        onClick = {
                            editingAppointment = null
                            showAddEditSheet = true
                        },
                        modifier = Modifier
                            .testTag("add_appointment_fab")
                            .padding(bottom = 12.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "إضافة موعد")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "إضافة موعد",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                // Dynamic Spotlight Card & Stats Row
                SpotlightAndStatsSection(
                    upcomingAppointments = appointmentsState.upcoming,
                    upcomingCount = upcomingCount,
                    completedCount = completedCount,
                    totalCount = totalCount
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Modern Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("ابحث عن موعد بالاسم أو التفاصيل...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input"),
                    shape = RoundedCornerShape(24.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح")
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Custom Tab filters (Upcoming / Completed History)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val activeBgColorUpcoming = if (selectedTab == "upcoming") MaterialTheme.colorScheme.primary else Color.Transparent
                    val activeTxtColorUpcoming = if (selectedTab == "upcoming") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    
                    val activeBgColorCompleted = if (selectedTab == "completed") MaterialTheme.colorScheme.primary else Color.Transparent
                    val activeTxtColorCompleted = if (selectedTab == "completed") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(activeBgColorUpcoming)
                            .clickable { selectedTab = "upcoming" }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "المواعيد النشطة ($upcomingCount)",
                            fontWeight = FontWeight.Bold,
                            color = activeTxtColorUpcoming,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(activeBgColorCompleted)
                            .clickable { selectedTab = "completed" }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "المنتهية والأرشيف ($completedCount)",
                            fontWeight = FontWeight.Bold,
                            color = activeTxtColorCompleted,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Selected Tab List
                val displayList = if (selectedTab == "upcoming") appointmentsState.upcoming else appointmentsState.past

                if (displayList.isEmpty()) {
                    EmptyStatePlaceholder(searchQuery.isNotEmpty())
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displayList, key = { it.id }) { appointment ->
                            AppointmentItemCard(
                                appointment = appointment,
                                onClick = {
                                    editingAppointment = appointment
                                    showAddEditSheet = true
                                },
                                onDelete = { viewModel.deleteAppointment(appointment) },
                                onToggleComplete = { viewModel.toggleCompletion(appointment) }
                            )
                        }
                    }
                }
            }

            // Bottom drawer edit/creator sheet
            if (showAddEditSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showAddEditSheet = false
                        soundPreviewHelper.stopPreviousPlayback()
                    },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    dragHandle = {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 12.dp)
                                .width(40.dp)
                                .height(4.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape)
                        )
                    }
                ) {
                    AppointmentFormSheetContent(
                        appointment = editingAppointment,
                        initialNotes = globalVoiceData,
                        soundPreviewHelper = soundPreviewHelper,
                        onDismiss = {
                            showAddEditSheet = false
                            globalVoiceData = ""
                            soundPreviewHelper.stopPreviousPlayback()
                        },
                        onSave = { updated ->
                            if (editingAppointment == null) {
                                viewModel.addAppointment(updated)
                            } else {
                                viewModel.updateAppointment(updated)
                            }
                            showAddEditSheet = false
                            globalVoiceData = ""
                            soundPreviewHelper.stopPreviousPlayback()
                        }
                    )
                }
            }
        }
    }
}

// TOP TITLE BAR WITH PRETTY HEADER
@Composable
fun HeaderSection(
    darkThemeOverride: Boolean?,
    onThemeToggle: (Boolean?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 16.dp, start = 20.dp, end = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "جدولة ذكية",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "مهامي ومواعيدي",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Luxurious Quick Theme Selector Component (Cycle between Auto, Light, Dark)
            val icon = when (darkThemeOverride) {
                true -> Icons.Default.DarkMode
                false -> Icons.Default.LightMode
                else -> Icons.Default.Settings
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .clickable {
                        val next = when (darkThemeOverride) {
                            null -> false // Go to Light
                            false -> true // Go to Dark
                            true -> null // Go back to System Auto
                        }
                        onThemeToggle(next)
                    }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = "تغيير المظهر",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Beautiful decorative user circle
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha=0.6f), MaterialTheme.colorScheme.tertiary.copy(alpha=0.6f))))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.Center).size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

// DYNAMIC SPOTLIGHT HERO & COMPACT STATISTICS SECTION
@Composable
fun SpotlightAndStatsSection(
    upcomingAppointments: List<Appointment>,
    upcomingCount: Int,
    completedCount: Int,
    totalCount: Int
) {
    val nextAppointment = upcomingAppointments.firstOrNull()

    if (nextAppointment != null) {
        val formattedRemaining = remember(nextAppointment.epochMillis) {
            val diffMs = nextAppointment.epochMillis - System.currentTimeMillis()
            if (diffMs <= 0) {
                "الآن"
            } else {
                val diffMin = diffMs / (1000 * 60)
                if (diffMin < 60) {
                    "بعد $diffMin دقيقة"
                } else {
                    val diffHour = diffMin / 60
                    if (diffHour < 24) {
                        "بعد $diffHour ساعة"
                    } else {
                        val diffDays = diffHour / 24
                        "بعد $diffDays يوم"
                    }
                }
            }
        }

        val triggerLabel = remember(nextAppointment.epochMillis, nextAppointment.alertOffsetMinutes) {
            val triggerTime = nextAppointment.epochMillis - (nextAppointment.alertOffsetMinutes * 60_000L)
            val sdf = SimpleDateFormat("hh:mm a", java.util.Locale("ar"))
            "تذكير: " + sdf.format(Date(triggerTime))
        }

        val alertLabel = when (nextAppointment.alertType) {
            "VOICE" -> "تنبيه برنين (${getSoundLabel(nextAppointment.soundTone)})"
            "TTS" -> "قارئ صوتي ناطق"
            "VIBRATE" -> "اهتزاز تفاعلي"
            else -> "إشعار مرئي صامت"
        }

        // Spotlight Card: Next Appointment Hero
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                        )
                    )
                    .padding(24.dp)
            ) {
                // Background decorative circle glow
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .align(Alignment.TopEnd)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.05f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.BottomStart)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.05f), CircleShape)
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.25f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "الموعد القادم",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            formattedRemaining,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.testTag("remaining_time_label")
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        nextAppointment.title,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (nextAppointment.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            nextAppointment.description,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Pulse control
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseAlpha"
                    )

                    // Custom Alert Info Bar within card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (nextAppointment.alertType == "VOICE") Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.Notifications,
                                contentDescription = null,
                                sizeModifier = Modifier.size(20.dp),
                                colorTint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "تنبيه ذكي نشط",
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "$alertLabel - $triggerLabel",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Pulser dot and accent
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = pulseAlpha))
                        )
                    }
                }
            }
        }
    } else {
        // Fallback Default Card when no upcoming items: Simple Statistics representation
        val gradientBrush = Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary
            )
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(gradientBrush)
                    .fillMaxWidth()
                    .padding(32.dp)
            ) {
                Text(
                    "إحصائيات الإنجاز والجدولة",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(title = "قادمة", count = upcomingCount.toString(), textColor = Color.White)
                    StatItem(title = "مكتملة", count = completedCount.toString(), textColor = Color.White)
                    StatItem(title = "إجمالي", count = totalCount.toString(), textColor = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
fun StatItem(title: String, count: String, textColor: Color) {
    Column {
        Text(
            count,
            color = textColor,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            title,
            color = textColor.copy(alpha = 0.8f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// EMPTY STATE PLACEHOLDER WITH CLEAN Arabic MASSAGE AND CHIP-BREAKING CANVAS
@Composable
fun EmptyStatePlaceholder(isFilterSearch: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isFilterSearch) Icons.Default.Search else Icons.AutoMirrored.Filled.EventNote,
                contentDescription = "لا يوجد مواعيد",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(46.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            if (isFilterSearch) "لا توجد نتائج" else "يومك صافي ومستعد",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            if (isFilterSearch) "جرب كلمات مختلفة في البحث للتأكد من وجود الموعد." else "سجل أول مواعيدك لنبدأ في تنظيم وإدارة أوقاتك بكل احترافية وذكاء.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

// Helper parser to get Arabic format digital/period parts
fun getHourAndAmPm(epochMillis: Long): Pair<String, String> {
    val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
    val hour24 = cal.get(Calendar.HOUR_OF_DAY)
    val hour12 = cal.get(Calendar.HOUR)
    val num = if (hour12 == 0) "12" else hour12.toString()
    val isPmStr = if (hour24 >= 12) "مساءً" else "صباحاً"
    return Pair(num, isPmStr)
}

// MODERN CARD FOR REMINDER APPOINTMENT CARD
@Composable
fun AppointmentItemCard(
    appointment: Appointment,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleComplete: () -> Unit
) {
    val modeColor = when (appointment.alertType) {
        "VOICE" -> MaterialTheme.colorScheme.primary
        "TTS" -> MaterialTheme.colorScheme.secondary
        "VIBRATE" -> MaterialTheme.colorScheme.tertiary
        else -> Color.Gray
    }

    val formattedDate = remember(appointment.epochMillis) {
        val sdf = SimpleDateFormat("dd MMMM yyyy", java.util.Locale("ar"))
        sdf.format(Date(appointment.epochMillis))
    }

    val triggerLabel = remember(appointment.epochMillis, appointment.alertOffsetMinutes) {
        val triggerTime = appointment.epochMillis - (appointment.alertOffsetMinutes * 60_000L)
        val sdf = SimpleDateFormat("hh:mm a", java.util.Locale("ar"))
        "تذكير: " + sdf.format(Date(triggerTime))
    }

    val shortAlertLabel = when (appointment.alertType) {
        "VOICE" -> "تنبيه برنين"
        "TTS" -> "قارئ صوتي"
        "VIBRATE" -> "اهتزاز تفاعلي"
        else -> "إشعار صامت"
    }

    val timeInfo = remember(appointment.epochMillis) { getHourAndAmPm(appointment.epochMillis) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .animateContentSize()
            .testTag("appointment_item_${appointment.id}"),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant time period container matching user's design template
            Box(
                modifier = Modifier
                    .size(height = 56.dp, width = 56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (appointment.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        timeInfo.first,
                        color = if (appointment.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        timeInfo.second,
                        color = if (appointment.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text detail contents center
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Accent mode indicator bar
                    Box(
                        modifier = Modifier
                            .size(width = 4.dp, height = 14.dp)
                            .clip(CircleShape)
                            .background(if (appointment.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha=0.3f) else modeColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        appointment.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (appointment.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (appointment.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        appointment.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    formattedDate,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right-aligned metadata layout matches HTML spec
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    shortAlertLabel,
                    fontSize = 12.sp,
                    color = if (appointment.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    triggerLabel,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onToggleComplete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("checkbox_${appointment.id}")
                    ) {
                        Icon(
                            imageVector = if (appointment.isCompleted) Icons.Default.CheckCircle else Icons.Default.Circle,
                            contentDescription = "تعديل الحالة",
                            sizeModifier = Modifier.size(22.dp),
                            colorTint = if (appointment.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_button_${appointment.id}")
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "مسح الموعد",
                                sizeModifier = Modifier.size(16.dp),
                                colorTint = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getSoundLabel(sound: String): String {
    return when (sound) {
        "DEFAULT" -> "الافتراضي"
        "DIGITAL_BEEP" -> "رنين ذكي"
        "SOFT_BELL" -> "جرس هادئ"
        "CHIPTUNE" -> "أركيد الكتروني"
        else -> sound
    }
}

// BOTTOM SHEET FORM CONTENT FOR CREATION/EDITION
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppointmentFormSheetContent(
    appointment: Appointment?,
    initialNotes: String = "",
    soundPreviewHelper: SoundPreviewHelper,
    onDismiss: () -> Unit,
    onSave: (Appointment) -> Unit
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    // Form Field States
    var title by remember { mutableStateOf(appointment?.title ?: "") }
    var description by remember { mutableStateOf(appointment?.description ?: initialNotes) }
    var epochMillis by remember { mutableLongStateOf(appointment?.epochMillis ?: System.currentTimeMillis()) }
    var alertOffsetMinutes by remember { mutableIntStateOf(appointment?.alertOffsetMinutes ?: 0) }
    var alertType by remember { mutableStateOf(appointment?.alertType ?: "VOICE") }
    var soundTone by remember { mutableStateOf(appointment?.soundTone ?: "DEFAULT") }

    // Speech Recognizer setup for title
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = matches?.firstOrNull() ?: ""
            if (recognizedText.isNotEmpty()) {
                title = recognizedText
            }
        }
    }

    // Speech Recognizer setup for description
    val descSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = matches?.firstOrNull() ?: ""
            if (recognizedText.isNotEmpty()) {
                description = recognizedText
            }
        }
    }

    // Title validation boundary
    var isTitleError by remember { mutableStateOf(false) }

    // Keep calendar synchronised with epochMillis
    calendar.timeInMillis = epochMillis

    // Clean Date / Time displays
    val showDate = remember(epochMillis) {
        val sdf = SimpleDateFormat("dd MMMM yyyy", java.util.Locale("ar"))
        sdf.format(Date(epochMillis))
    }
    val showTime = remember(epochMillis) {
        val sdf = SimpleDateFormat("hh:mm a", java.util.Locale("ar"))
        sdf.format(Date(epochMillis))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            if (appointment == null) "جدولة موعد جديد" else "تعديل الموعد المجدول",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(14.dp))

        // Title textfield
        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                if (isTitleError && it.isNotEmpty()) isTitleError = false
            },
            label = { Text("اسم الموعد / الغرض") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("appointment_title_input"),
            shape = RoundedCornerShape(20.dp),
            isError = isTitleError,
            leadingIcon = { Icon(Icons.Default.TextFormat, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث لتسجيل اسم الموعد...")
                    }
                    try {
                        speechRecognizerLauncher.launch(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "التعرف على الصوت غير مدعوم في جهازك", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(Icons.Default.Mic, contentDescription = "إدخال صوتي", tint = MaterialTheme.colorScheme.primary)
                }
            },
            singleLine = true
        )
        if (isTitleError) {
            Text(
                "يرجى تحديد اسم الموعد لحفظه بشكل سليم",
                color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Description textfield
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("تفاصيل وملاحظات الموعد...") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("appointment_desc_input"),
            shape = RoundedCornerShape(20.dp),
            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث لتسجيل تفاصيل الموعد...")
                    }
                    try {
                        descSpeechLauncher.launch(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "التعرف على الصوت غير مدعوم", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(Icons.Default.Mic, contentDescription = "إدخال صوتي", tint = MaterialTheme.colorScheme.primary)
                }
            },
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Date and Time selectors inside structured Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // DATE BUTTON
            Button(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            calendar.set(Calendar.YEAR, year)
                            calendar.set(Calendar.MONTH, month)
                            calendar.set(Calendar.DAY_OF_MONTH, day)
                            epochMillis = calendar.timeInMillis
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .testTag("date_picker_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(showDate, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // TIME BUTTON
            Button(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            calendar.set(Calendar.HOUR_OF_DAY, hour)
                            calendar.set(Calendar.MINUTE, minute)
                            calendar.set(Calendar.SECOND, 0)
                            epochMillis = calendar.timeInMillis
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        false // Set true if you want 24 hours format
                    ).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .testTag("time_picker_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                    contentColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Alarm, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(showTime, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // REMINDER TIMING (METHA YUNBIHONI)
        Text(
            "متى ترغب في التنبيه؟",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        
        val reminderBaseMillis = epochMillis - (alertOffsetMinutes * 60_000L)
        val reminderCalendar = remember(reminderBaseMillis) { Calendar.getInstance().apply { timeInMillis = reminderBaseMillis } }

        val showReminderDate = remember(reminderBaseMillis) {
            val sdf = SimpleDateFormat("dd MMMM yyyy", java.util.Locale("ar"))
            sdf.format(Date(reminderBaseMillis))
        }
        val showReminderTime = remember(reminderBaseMillis) {
            val sdf = SimpleDateFormat("hh:mm a", java.util.Locale("ar"))
            sdf.format(Date(reminderBaseMillis))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // REMINDER DATE BUTTON
            Button(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            val cal = Calendar.getInstance().apply { timeInMillis = reminderBaseMillis }
                            cal.set(Calendar.YEAR, year)
                            cal.set(Calendar.MONTH, month)
                            cal.set(Calendar.DAY_OF_MONTH, day)
                            alertOffsetMinutes = ((epochMillis - cal.timeInMillis) / 60_000L).toInt()
                        },
                        reminderCalendar.get(Calendar.YEAR),
                        reminderCalendar.get(Calendar.MONTH),
                        reminderCalendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(showReminderDate, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // REMINDER TIME BUTTON
            Button(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            val cal = Calendar.getInstance().apply { timeInMillis = reminderBaseMillis }
                            cal.set(Calendar.HOUR_OF_DAY, hour)
                            cal.set(Calendar.MINUTE, minute)
                            cal.set(Calendar.SECOND, 0)
                            alertOffsetMinutes = ((epochMillis - cal.timeInMillis) / 60_000L).toInt()
                        },
                        reminderCalendar.get(Calendar.HOUR_OF_DAY),
                        reminderCalendar.get(Calendar.MINUTE),
                        false // Set true if you want 24 hours format
                    ).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Alarm, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(showReminderTime, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ALERT METHOD PREFERENCE (ALEIYAT AL-TANBEEH)
        Text(
            "آلية التنبيه المفضلة:",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))

        val alertTypesList = listOf(
            "VOICE" to ("تنبيه صوتي" to Icons.AutoMirrored.Filled.VolumeUp),
            "TTS" to ("قارئ منطوق" to Icons.Default.Notifications),
            "VIBRATE" to ("اهتزاز تفاعلي" to Icons.AutoMirrored.Filled.VolumeUp),
            "SILENT" to ("إشعار صامت" to Icons.Default.Notifications)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            alertTypesList.forEach { (key, pair) ->
                val (lbl, icon) = pair
                val selected = alertType == key
                val bg = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                val tc = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(bg)
                        .clickable { alertType = key }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(icon, contentDescription = null, sizeModifier = Modifier.size(20.dp), colorTint = tc)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(lbl, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = tc)
                    }
                }
            }
        }

        // TONE OPTION TO CUSTOMIZE TONE AUDIO
        if (alertType == "VOICE") {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                "رنين التنبيه المخصص:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            val tonesList = listOf(
                "DEFAULT" to "الافتراضي",
                "DIGITAL_BEEP" to "رنين ذكي",
                "SOFT_BELL" to "جرس ناعم",
                "CHIPTUNE" to "الكتروني"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tonesList.forEach { (key, lbl) ->
                    val selected = soundTone == key
                    val bg = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                    val tc = if (selected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .clickable { soundTone = key }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(lbl, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tc)
                    }
                }
            }
        }

        // AUDIO TEST DRIVE ACTION BUTTON ("تخصيصه بنفسي")
        if (alertType == "VOICE" || alertType == "TTS" || alertType == "VIBRATE") {
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = {
                    soundPreviewHelper.playPreview(
                        alertType = alertType,
                        soundTone = soundTone,
                        title = title.ifEmpty { "موعد جديد" }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("preview_sound_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "تجربة وتخصيص صوت المنبه الآن 🎧",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SAVE AND CANCEL BUTTONS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        isTitleError = true
                    } else {
                        val toSave = Appointment(
                            id = appointment?.id ?: 0,
                            title = title,
                            description = description,
                            epochMillis = epochMillis,
                            alertOffsetMinutes = alertOffsetMinutes,
                            alertType = alertType,
                            soundTone = soundTone,
                            isCompleted = appointment?.isCompleted ?: false,
                            isTriggered = appointment?.isTriggered ?: false
                        )
                        onSave(toSave)
                    }
                },
                modifier = Modifier
                    .weight(1.5f)
                    .height(56.dp)
                    .testTag("save_form_button"),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("حفظ الموعد والمنبه", fontWeight = FontWeight.Black, fontSize = 15.sp)
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("إلغاء", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// Helper icons wrapper to maintain material component size limits and clean tinting
@Composable
fun Icon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    sizeModifier: Modifier = Modifier,
    colorTint: Color = MaterialTheme.colorScheme.onSurface
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = sizeModifier,
            tint = colorTint
        )
    }
}
