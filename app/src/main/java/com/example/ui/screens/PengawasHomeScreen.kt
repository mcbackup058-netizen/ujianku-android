package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.QuestionEntity
import com.example.ui.theme.*
import com.example.viewmodel.UjianScreen
import com.example.viewmodel.UjianViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PengawasHomeScreen(viewModel: UjianViewModel) {
    val currentProctor by viewModel.currentProctor.collectAsStateWithLifecycle()
    val allLogs by viewModel.allActivityLogs.collectAsStateWithLifecycle()
    val examsList by viewModel.exams.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 = Daftar Ujian, 1 = Live Infractions
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("Semua") }
    var showCreateExamDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                DarkSmoothGreen,
                                SoftGreen
                            )
                        ),
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(top = 20.dp, bottom = 28.dp, start = 20.dp, end = 20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(Color.White.copy(alpha = 0.18f), CircleShape)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    "Pengawas Dashboard Sesi",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    currentProctor?.name ?: "Pengawas Utama",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.logout() },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Keluar",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Supervising metadata layout
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("NIP PENGAWAS", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                            Text(currentProctor?.nip ?: "-", style = MaterialTheme.typography.labelMedium, color = Color.White)
                        }
                        Column {
                            Text("SEKOLAH INDUK", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                            Text(currentProctor?.school ?: "-", style = MaterialTheme.typography.labelMedium, color = Color.White)
                        }
                        Column {
                            Text("SUPERVISI RUANG", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                            Text(currentProctor?.supervisedClass ?: "-", style = MaterialTheme.typography.labelMedium, color = Color.White)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Operational dynamic metrics cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    Triple("Ujian Dipantau", "${examsList.size}", MaterialTheme.colorScheme.primary),
                    Triple("Pelanggaran", "${allLogs.filter { it.type == "FOCUS_LOST" }.size}", MaterialTheme.colorScheme.error),
                    Triple("Submisi Masuk", "${allLogs.filter { it.type == "SUBMIT" }.size}", MaterialTheme.colorScheme.primary)
                ).forEach { (title, count, color) ->
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(count, fontSize = 20.sp, fontWeight = FontWeight.Black, color = color)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            CloudSyncStatusCard(viewModel = viewModel)

            Spacer(modifier = Modifier.height(18.dp))

            // Modern sliding tab select
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (activeTab == 0) Color.White else Color.Transparent)
                        .clickable { activeTab = 0 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Jadwal Lembar Pengawasan", style = MaterialTheme.typography.labelMedium, color = if (activeTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (activeTab == 1) Color.White else Color.Transparent)
                        .clickable { activeTab = 1 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Live Pelanggaran Integritas", style = MaterialTheme.typography.labelMedium, color = if (activeTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (activeTab == 0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lembar Ujian Pengawasan",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Button(
                        onClick = { showCreateExamDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp).testTag("add_exam_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah Ujian Baru", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Dynamic create exam dialog with custom question builder
                if (showCreateExamDialog) {
                    CreateExamDialog(
                        onDismiss = { showCreateExamDialog = false },
                        onCreateExam = { title, subject, duration, token, questions ->
                            viewModel.createNewExam(title, subject, duration, token, questions)
                            showCreateExamDialog = false
                        }
                    )
                }

                // Exam supervision list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(examsList) { exam ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        exam.subject.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (exam.isFinished) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (exam.isFinished) "UJIAN SELESAI" else "AKTIF BERJALAN",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (exam.isFinished) MaterialTheme.colorScheme.primary else PrimaryDark
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    exam.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "TOKEN: ${exam.tokenCode}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    
                                    Button(
                                        onClick = { activeTab = 1 }, // Switch over to check focus logs
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Pantau Aktivitas", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Focus Lost Live Logs panel (surveillance layout)
                val filteredLogs = allLogs.filter { log ->
                    val matchesSearch = log.studentName.contains(searchQuery, ignoreCase = true) || 
                                        log.message.contains(searchQuery, ignoreCase = true) ||
                                        log.examTitle.contains(searchQuery, ignoreCase = true)
                    val matchesCategory = when (selectedCategoryFilter) {
                        "Fokus Lepas" -> log.type == "FOCUS_LOST"
                        "Submisi" -> log.type == "SUBMIT"
                        "Mulai Ujian" -> log.type == "START_EXAM"
                        else -> true
                    }
                    matchesSearch && matchesCategory
                }

                Column(modifier = Modifier.weight(1f)) {
                    // Search and Filter Bar Row
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari Siswa, Ujian, atau Aktivitas...", fontSize = 12.sp) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    // Category Chip Filters
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Semua", "Fokus Lepas", "Submisi", "Mulai Ujian").forEach { cat ->
                            val isSelected = selectedCategoryFilter == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                    .clickable { selectedCategoryFilter = cat }
                                    .border(BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (filteredLogs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                        )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "AKTIVITAS AMAN ATAU KOSONG",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    "Tidak ada catatan log aktivitas yang cocok dengan kriteria filter.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredLogs) { log ->
                                val cardBorderColor = when (log.type) {
                                    "FOCUS_LOST" -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                                    "SUBMIT" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                }
                                val iconColor = when (log.type) {
                                    "FOCUS_LOST" -> MaterialTheme.colorScheme.error
                                    "SUBMIT" -> MaterialTheme.colorScheme.primary
                                    else -> PrimaryDark
                                }
                                val iconVector = when (log.type) {
                                    "FOCUS_LOST" -> Icons.Default.Warning
                                    "SUBMIT" -> Icons.Default.CheckCircle
                                    else -> Icons.Default.PlayCircle
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(BorderStroke(1.2.dp, cardBorderColor), RoundedCornerShape(12.dp)),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = iconVector,
                                                    contentDescription = null,
                                                    tint = iconColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    log.studentName,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = iconColor
                                                )
                                            }

                                            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                            Text(
                                                text = sdf.format(Date(log.timestamp)),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = log.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.Book, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Ujian: ${log.examTitle}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Premium reset helper button at bottom in dark border
            OutlinedButton(
                onClick = { viewModel.resetDemoData() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Seed / Reset Sesi Uji Coba", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * Dynamic Create Exam Dialog with custom question builder.
 * Replaces the hardcoded Physics questions with the ability to add custom questions
 * with 5 options (A-E) and a correct answer selector.
 */
@Composable
private fun CreateExamDialog(
    onDismiss: () -> Unit,
    onCreateExam: (String, String, Int, String, List<QuestionEntity>) -> Unit
) {
    var newSubject by remember { mutableStateOf("") }
    var newTitle by remember { mutableStateOf("") }
    var newDuration by remember { mutableStateOf("15") }
    var newToken by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Dynamic questions list
    var questions by remember { mutableStateOf(listOf<DynamicQuestion>()) }

    // Add a default first question
    LaunchedEffect(Unit) {
        if (questions.isEmpty()) {
            questions = listOf(DynamicQuestion())
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Buat Paket Ujian Baru",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Masukkan spesifikasi detail lembar ujian dan buat soal kustom Anda.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = newSubject,
                    onValueChange = { newSubject = it },
                    label = { Text("Mata Pelajaran") },
                    placeholder = { Text("Contoh: Fisika, Biologi") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Judul Ujian") },
                    placeholder = { Text("Contoh: Ulangan Harian Dinamika Partikel") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newDuration,
                        onValueChange = { newDuration = it },
                        label = { Text("Durasi (Menit)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )
                    OutlinedTextField(
                        value = newToken,
                        onValueChange = { newToken = it },
                        label = { Text("Token") },
                        placeholder = { Text("Contoh: FISIKA99") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.2f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )
                }

                // Questions Section Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAFTAR SOAL (${questions.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                    IconButton(
                        onClick = { questions = questions + DynamicQuestion() },
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah Soal",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Dynamic question cards
                questions.forEachIndexed { index, q ->
                    DynamicQuestionCard(
                        question = q,
                        questionNumber = index + 1,
                        onUpdate = { updatedQ ->
                            questions = questions.toMutableList().also { it[index] = updatedQ }
                        },
                        onRemove = {
                            questions = questions.toMutableList().also { it.removeAt(index) }
                        },
                        canRemove = questions.size > 1
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                errorMsg?.let { msg ->
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val dur = newDuration.toIntOrNull()
                            if (newSubject.isBlank() || newTitle.isBlank() || newToken.isBlank()) {
                                errorMsg = "Semua kolom harus diisi!"
                            } else if (dur == null || dur <= 0) {
                                errorMsg = "Durasi harus positif!"
                            } else if (questions.any { it.text.isBlank() || it.optionA.isBlank() || it.optionB.isBlank() || it.optionC.isBlank() || it.optionD.isBlank() || it.optionE.isBlank() }) {
                                errorMsg = "Semua soal dan opsi jawaban harus diisi!"
                            } else if (questions.any { it.correctOption.isBlank() }) {
                                errorMsg = "Setiap soal harus memiliki kunci jawaban yang dipilih!"
                            } else {
                                val qList = questions.mapIndexed { i, dq ->
                                    QuestionEntity(
                                        examId = "",
                                        questionNumber = i + 1,
                                        text = dq.text,
                                        optionA = dq.optionA,
                                        optionB = dq.optionB,
                                        optionC = dq.optionC,
                                        optionD = dq.optionD,
                                        optionE = dq.optionE,
                                        correctOption = dq.correctOption
                                    )
                                }
                                onCreateExam(newTitle, newSubject, dur, newToken, qList)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Buat Sekarang", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

/**
 * Data class to hold a dynamic question's state in the dialog
 */
private data class DynamicQuestion(
    val text: String = "",
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",
    val optionE: String = "",
    val correctOption: String = ""
)

/**
 * Composable for rendering a single dynamic question input card
 */
@Composable
private fun DynamicQuestionCard(
    question: DynamicQuestion,
    questionNumber: Int,
    onUpdate: (DynamicQuestion) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Soal No. $questionNumber",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (canRemove) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Soal",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = question.text,
                onValueChange = { onUpdate(question.copy(text = it)) },
                label = { Text("Teks Pertanyaan", fontSize = 12.sp) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                minLines = 2,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
            )

            // Options A-E in a compact layout
            val optionLabels = listOf("A", "B", "C", "D", "E")
            val optionValues = listOf(question.optionA, question.optionB, question.optionC, question.optionD, question.optionE)
            val optionUpdaters: List<(String) -> DynamicQuestion> = listOf(
                { v: String -> question.copy(optionA = v) },
                { v: String -> question.copy(optionB = v) },
                { v: String -> question.copy(optionC = v) },
                { v: String -> question.copy(optionD = v) },
                { v: String -> question.copy(optionE = v) }
            )

            optionLabels.forEachIndexed { i, label ->
                val isSelected = question.correctOption == label
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Correct answer radio button
                    IconButton(
                        onClick = { onUpdate(question.copy(correctOption = label)) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Kunci jawaban: $label",
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    OutlinedTextField(
                        value = optionValues[i],
                        onValueChange = { onUpdate(optionUpdaters[i](it)) },
                        label = { Text("Opsi $label", fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            unfocusedBorderColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }

            // Show selected correct answer
            if (question.correctOption.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Kunci Jawaban: ${question.correctOption}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
