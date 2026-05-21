package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
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
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
                FloatingActionButton(
                    onClick = {
                        editingAppointment = null
                        showAddEditSheet = true
                    },
                    modifier = Modifier
                        .testTag("add_appointment_fab")
                        .padding(bottom = 16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة موعد")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "إضافة موعد",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
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
                    shape = RoundedCornerShape(16.dp),
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

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Tab filters (Upcoming / Completed History)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(activeBgColorUpcoming)
                            .clickable { selectedTab = "upcoming" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "المواعيد النشطة ($upcomingCount)",
                            fontWeight = FontWeight.Bold,
                            color = activeTxtColorUpcoming,
                            fontSize = 14.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(activeBgColorCompleted)
                            .clickable { selectedTab = "completed" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "المتتهية والأرشيف ($completedCount)",
                            fontWeight = FontWeight.Bold,
                            color = activeTxtColorCompleted,
                            fontSize = 14.sp
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
                        soundPreviewHelper = soundPreviewHelper,
                        onDismiss = {
                            showAddEditSheet = false
                            soundPreviewHelper.stopPreviousPlayback()
                        },
                        onSave = { updated ->
                            if (editingAppointment == null) {
                                viewModel.addAppointment(updated)
                            } else {
                                viewModel.updateAppointment(updated)
                            }
                            showAddEditSheet = false
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
            .padding(top = 16.dp, bottom = 8.dp, start = 20.dp, end = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "مدير المهام",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "المواعيد القادمة",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
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
            val label = when (darkThemeOverride) {
                true -> "داكن"
                false -> "فاتح"
                else -> "تلقائي"
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .clickable {
                        val next = when (darkThemeOverride) {
                            null -> false // Go to Light
                            false -> true // Go to Dark
                            true -> null // Go back to System Auto
                        }
                        onThemeToggle(next)
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        icon,
                        contentDescription = "تغيير المظهر",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Beautiful decorative user circle from HTML: bg-[#4F378B] with outer border-[#938F99] and internal circle ring
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4F378B))
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .background(Brush.linearGradient(listOf(Color(0xFFEADDFF), Color(0xFFD0BCFF))))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF4F378B))
                    )
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

        val triggerLabel = when (nextAppointment.alertOffsetMinutes) {
            0 -> "في نفس الوقت بالضبط"
            5 -> "قبل بـ ٥ دقائق"
            15 -> "قبل بـ ١٥ دقيقة"
            30 -> "قبل بـ ٣٠ دقيقة"
            60 -> "قبل بساعة"
            120 -> "قبل بساعتين"
            1440 -> "قبل بيوم"
            else -> "قبل بـ ${nextAppointment.alertOffsetMinutes} دقائق"
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
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF4F378B))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF6750A4), Color(0xFF4F378B)),
                            radius = 600f
                        )
                    )
                    .padding(24.dp)
            ) {
                // Background decorative circle glow
                Box(
                    modifier = Modifier
                        .size(128.dp)
                        .align(Alignment.TopEnd)
                        .background(Color(0xFFD0BCFF).copy(alpha = 0.08f), CircleShape)
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFEADDFF))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "الموعد التالي",
                                color = Color(0xFF21005D),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        
                        Text(
                            formattedRemaining,
                            color = Color(0xFFEADDFF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.testTag("remaining_time_label")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        nextAppointment.title,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Light,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (nextAppointment.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            nextAppointment.description,
                            color = Color(0xFFEADDFF).copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
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
                            animation = tween(durationMillis = 1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseAlpha"
                    )

                    // Custom Alert Info Bar within card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF381E72).copy(alpha = 0.40f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFD0BCFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (nextAppointment.alertType == "VOICE") Icons.Default.VolumeUp else Icons.Default.Notifications,
                                contentDescription = null,
                                sizeModifier = Modifier.size(18.dp),
                                colorTint = Color(0xFF381E72)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "تنبيه مخصص نشط",
                                color = Color(0xFFEADDFF).copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "$alertLabel - $triggerLabel",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Pulser dot and accent
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD0BCFF).copy(alpha = pulseAlpha))
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
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(gradientBrush)
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    "أداء المواعيد والجدولة اليومية",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(title = "المواعيد القادمة", count = upcomingCount.toString(), textColor = Color.White)
                    StatItem(title = "المكتملة بنجاح", count = completedCount.toString(), textColor = Color.White)
                    StatItem(title = "إجمالي المحفوظ", count = totalCount.toString(), textColor = Color.White.copy(alpha = 0.8f))
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
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            title,
            color = textColor.copy(alpha = 0.7f),
            fontSize = 11.sp,
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
        Icon(
            imageVector = if (isFilterSearch) Icons.Default.Search else Icons.Default.EventNote,
            contentDescription = "لا يوجد مواعيد",
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            if (isFilterSearch) "لا توجد نتائج مطابقة لبحثك" else "سجل مواعيدك الآن!",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            if (isFilterSearch) "تأكد من كتابة اسم الموعد بشكل صحيح وإعادة المحاولة." else "قم بإضافة موعدك الأول وخصص نغمات تنبيه وتوقيتات تفاعلية بنفسك.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
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
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("ar"))
        sdf.format(Date(appointment.epochMillis))
    }

    val triggerLabel = when (appointment.alertOffsetMinutes) {
        0 -> "في الموعد بالضبط"
        5 -> "قبل بـ ٥ دقائق"
        15 -> "قبل بـ ١٥ دقيقة"
        30 -> "قبل بـ ٣٠ دقيقة"
        60 -> "قبل بساعة"
        120 -> "قبل بساعتين"
        1440 -> "قبل بيوم"
        else -> "قبل بـ ${appointment.alertOffsetMinutes} دقيقة"
    }

    val shortAlertLabel = when (appointment.alertType) {
        "VOICE" -> "تنبيه برنين"
        "TTS" -> "قارئ صوتي"
        "VIBRATE" -> "اهتزاز تفاعلي"
        else -> "إشعار صامت"
    }

    val timeInfo = remember(appointment.epochMillis) { getHourAndAmPm(appointment.epochMillis) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("appointment_item_${appointment.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant time period container matching user's design template
            Box(
                modifier = Modifier
                    .size(height = 48.dp, width = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF332D41))
                    .border(BorderStroke(1.dp, Color(0xFF49454F)), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        timeInfo.first,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        timeInfo.second,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

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
                            .size(width = 3.dp, height = 12.dp)
                            .clip(CircleShape)
                            .background(modeColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        appointment.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (appointment.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (appointment.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        appointment.description,
                        fontSize = 12.sp,
                        color = Color(0xFF938F99),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    formattedDate,
                    fontSize = 10.sp,
                    color = Color(0xFF938F99),
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
                    fontSize = 11.sp,
                    color = Color(0xFFD0BCFF),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    triggerLabel,
                    fontSize = 9.sp,
                    color = Color(0xFF938F99),
                    fontWeight = FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onToggleComplete,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("checkbox_${appointment.id}")
                    ) {
                        Icon(
                            imageVector = if (appointment.isCompleted) Icons.Default.CheckCircle else Icons.Default.Circle,
                            contentDescription = "تعديل الحالة",
                            sizeModifier = Modifier.size(18.dp),
                            colorTint = if (appointment.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("delete_button_${appointment.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "مسح الموعد",
                            sizeModifier = Modifier.size(18.dp),
                            colorTint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
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
    soundPreviewHelper: SoundPreviewHelper,
    onDismiss: () -> Unit,
    onSave: (Appointment) -> Unit
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    // Form Field States
    var title by remember { mutableStateOf(appointment?.title ?: "") }
    var description by remember { mutableStateOf(appointment?.description ?: "") }
    var epochMillis by remember { mutableLongStateOf(appointment?.epochMillis ?: System.currentTimeMillis()) }
    var alertOffsetMinutes by remember { mutableIntStateOf(appointment?.alertOffsetMinutes ?: 0) }
    var alertType by remember { mutableStateOf(appointment?.alertType ?: "VOICE") }
    var soundTone by remember { mutableStateOf(appointment?.soundTone ?: "DEFAULT") }

    // Title validation boundary
    var isTitleError by remember { mutableStateOf(false) }

    // Keep calendar synchronised with epochMillis
    calendar.timeInMillis = epochMillis

    // Clean Date / Time displays
    val showDate = remember(epochMillis) {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("ar"))
        sdf.format(Date(epochMillis))
    }
    val showTime = remember(epochMillis) {
        val sdf = SimpleDateFormat("hh:mm a", Locale("ar"))
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
            shape = RoundedCornerShape(12.dp),
            isError = isTitleError,
            leadingIcon = { Icon(Icons.Default.TextFormat, contentDescription = null) },
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
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
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
                    .height(52.dp)
                    .testTag("date_picker_button"),
                shape = RoundedCornerShape(12.dp),
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
                    .height(52.dp)
                    .testTag("time_picker_button"),
                shape = RoundedCornerShape(12.dp),
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
        
        // Horizontal list of trigger time options
        val timingOptions = listOf(
            0 to "في نفس الوقت",
            5 to "قبل بـ 5 د",
            15 to "قبل بـ 15 د",
            30 to "قبل بـ 30 د",
            60 to "قبل بساعة",
            120 to "قبل بساعتين",
            1440 to "قبل بيوم"
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            timingOptions.forEach { (minutes, label) ->
                val selected = alertOffsetMinutes == minutes
                val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                val textColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(color)
                        .clickable { alertOffsetMinutes = minutes }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
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
            "VOICE" to ("تنبيه صوتي" to Icons.Default.VolumeUp),
            "TTS" to ("قارئ منطوق" to Icons.Default.Notifications),
            "VIBRATE" to ("اهتزاز فقط" to Icons.Default.VolumeUp),
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
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .clickable { alertType = key }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(icon, contentDescription = null, sizeModifier = Modifier.size(16.dp), colorTint = tc)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(lbl, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = tc)
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .clickable { soundTone = key }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(lbl, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tc)
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
                    .height(48.dp)
                    .testTag("preview_sound_button"),
                shape = RoundedCornerShape(12.dp),
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
                    .height(50.dp)
                    .testTag("save_form_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("حفظ الموعد والمنبه", fontWeight = FontWeight.Black, fontSize = 14.sp)
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إلغاء", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
