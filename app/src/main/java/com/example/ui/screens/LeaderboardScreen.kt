package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LeaderboardEntity
import com.example.ui.theme.*
import com.example.viewmodel.UjianScreen
import com.example.viewmodel.UjianViewModel

@Composable
fun LeaderboardScreen(viewModel: UjianViewModel) {
    val allLeaderboard by viewModel.leaderboardEntries.collectAsStateWithLifecycle()
    val examsList by viewModel.exams.collectAsStateWithLifecycle()
    val currentSiswa by viewModel.currentSiswa.collectAsStateWithLifecycle()
    
    var selectedExamFilter by remember { mutableStateOf<String?>(null) }

    // Filter leaderboard by exam if selected
    val filteredLeaderboard = if (selectedExamFilter != null) {
        allLeaderboard.filter { it.examId == selectedExamFilter }
    } else {
        allLeaderboard
    }

    // Sort by score descending
    val sortedLeaderboard = filteredLeaderboard.sortedByDescending { it.score }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top header bar
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
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                )
                .padding(top = 16.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.navigateTo(UjianScreen.SISWA_HOME) },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Papan Peringkat",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = SmoothYellow,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filter by exam dropdown
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Filter: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    // Filter chips
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // "Semua" chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selectedExamFilter == null) Color.White.copy(alpha = 0.3f)
                                    else Color.White.copy(alpha = 0.08f)
                                )
                                .clickable { selectedExamFilter = null }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Semua",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = if (selectedExamFilter == null) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        // Per-exam chips
                        examsList.forEach { exam ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selectedExamFilter == exam.id) Color.White.copy(alpha = 0.3f)
                                        else Color.White.copy(alpha = 0.08f)
                                    )
                                    .clickable { selectedExamFilter = exam.id }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = exam.subject,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = if (selectedExamFilter == exam.id) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        if (sortedLeaderboard.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Belum Ada Data Peringkat",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Selesaikan ujian untuk masuk ke papan peringkat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Podium-style top 3
                if (sortedLeaderboard.size >= 3) {
                    PodiumSection(
                        first = sortedLeaderboard[0],
                        second = sortedLeaderboard[1],
                        third = sortedLeaderboard[2],
                        currentSiswaNisn = currentSiswa?.nisn
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                } else if (sortedLeaderboard.isNotEmpty()) {
                    // Simplified display for less than 3 entries
                    sortedLeaderboard.take(3).forEachIndexed { index, entry ->
                        val medalColors = listOf(
                            Color(0xFFFFD700), // Gold
                            Color(0xFFC0C0C0), // Silver
                            Color(0xFFCD7F32)  // Bronze
                        )
                        PodiumEntryCard(
                            entry = entry,
                            rank = index + 1,
                            medalColor = medalColors.getOrElse(index) { MaterialTheme.colorScheme.primary },
                            isCurrentUser = entry.studentNisn == currentSiswa?.nisn
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Full ranking list header
                Text(
                    text = "PERINGKAT LENGKAP",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Full ranking list
                sortedLeaderboard.forEachIndexed { index, entry ->
                    RankingListCard(
                        entry = entry,
                        rank = index + 1,
                        isCurrentUser = entry.studentNisn == currentSiswa?.nisn
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PodiumSection(
    first: LeaderboardEntity,
    second: LeaderboardEntity,
    third: LeaderboardEntity,
    currentSiswaNisn: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        // 2nd place (Silver) - left
        PodiumColumn(
            entry = second,
            rank = 2,
            medalColor = Color(0xFFC0C0C0),
            medalIcon = Icons.Default.EmojiEvents,
            height = 120.dp,
            isCurrentUser = second.studentNisn == currentSiswaNisn
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 1st place (Gold) - center (tallest)
        PodiumColumn(
            entry = first,
            rank = 1,
            medalColor = Color(0xFFFFD700),
            medalIcon = Icons.Default.EmojiEvents,
            height = 150.dp,
            isCurrentUser = first.studentNisn == currentSiswaNisn
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 3rd place (Bronze) - right
        PodiumColumn(
            entry = third,
            rank = 3,
            medalColor = Color(0xFFCD7F32),
            medalIcon = Icons.Default.EmojiEvents,
            height = 100.dp,
            isCurrentUser = third.studentNisn == currentSiswaNisn
        )
    }
}

@Composable
private fun PodiumColumn(
    entry: LeaderboardEntity,
    rank: Int,
    medalColor: Color,
    medalIcon: androidx.compose.ui.graphics.vector.ImageVector,
    height: Dp,
    isCurrentUser: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        // Avatar with medal
        Box(
            modifier = Modifier
                .size(if (rank == 1) 64.dp else 52.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(medalColor.copy(alpha = 0.3f), medalColor.copy(alpha = 0.1f))
                    ),
                    CircleShape
                )
                .border(
                    BorderStroke(2.dp, medalColor),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = medalIcon,
                contentDescription = null,
                tint = medalColor,
                modifier = Modifier.size(if (rank == 1) 32.dp else 24.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = entry.studentName,
            style = MaterialTheme.typography.labelMedium,
            color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "${entry.score}",
            style = MaterialTheme.typography.titleMedium,
            color = medalColor,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Podium block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            medalColor.copy(alpha = 0.25f),
                            medalColor.copy(alpha = 0.08f)
                        )
                    ),
                    RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )
                .border(
                    BorderStroke(1.dp, medalColor.copy(alpha = 0.4f)),
                    RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.headlineMedium,
                color = medalColor,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun PodiumEntryCard(
    entry: LeaderboardEntity,
    rank: Int,
    medalColor: Color,
    isCurrentUser: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isCurrentUser) Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(14.dp)) else Modifier
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, medalColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(medalColor.copy(alpha = 0.15f), CircleShape)
                    .border(BorderStroke(1.5.dp, medalColor), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.labelLarge,
                    color = medalColor,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.studentName,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = entry.examTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${entry.score}",
                style = MaterialTheme.typography.titleLarge,
                color = medalColor,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun RankingListCard(
    entry: LeaderboardEntity,
    rank: Int,
    isCurrentUser: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isCurrentUser) Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(12.dp)) else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank number
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (isCurrentUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.studentName + if (isCurrentUser) " (Anda)" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium
                )
                Text(
                    text = entry.examTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Score visualization bar
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${entry.score}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black
                )
                // Mini score bar
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((entry.score / 100.0).coerceIn(0.0, 1.0).toFloat())
                            .background(
                                if (entry.score >= 80) SoftGreen
                                else if (entry.score >= 60) SmoothYellow
                                else WarningRed,
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
    }
}
