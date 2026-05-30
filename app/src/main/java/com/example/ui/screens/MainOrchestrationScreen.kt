package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.UjianScreen
import com.example.viewmodel.UjianViewModel

@Composable
fun MainOrchestrationScreen(viewModel: UjianViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

    MyApplicationThemeWrapper {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "ScreenNavigation"
            ) { screen ->
                when (screen) {
                    UjianScreen.SPLASH -> SplashScreen(viewModel)
                    UjianScreen.LOGIN -> LoginScreen(viewModel)
                    UjianScreen.SISWA_HOME -> SiswaHomeScreen(viewModel)
                    UjianScreen.EXAM_DETAIL -> ExamDetailScreen(viewModel)
                    UjianScreen.EXAM_TAKE -> ExamTakeScreen(viewModel)
                    UjianScreen.EXAM_RESULT -> ExamResultScreen(viewModel)
                    UjianScreen.PENGAWAS_HOME -> PengawasHomeScreen(viewModel)
                    UjianScreen.MONITORING -> MonitoringPlaceholderScreen(viewModel)
                    UjianScreen.VIOLATIONS -> ViolationsPlaceholderScreen(viewModel)
                    UjianScreen.LEADERBOARD -> LeaderboardScreen(viewModel)
                    UjianScreen.PROFILE -> ProfileScreen(viewModel)
                    UjianScreen.NOTIFICATIONS -> NotificationsScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun MonitoringPlaceholderScreen(viewModel: UjianViewModel) {
    ProctorPlaceholderScreen(
        viewModel = viewModel,
        title = "Pemantauan Ujian",
        subtitle = "Pantau sesi ujian yang sedang berlangsung secara real-time.",
        iconDescription = "Monitoring"
    )
}

@Composable
fun ViolationsPlaceholderScreen(viewModel: UjianViewModel) {
    ProctorPlaceholderScreen(
        viewModel = viewModel,
        title = "Pelanggaran Integritas",
        subtitle = "Daftar pelanggaran integritas akademik yang terdeteksi sistem.",
        iconDescription = "Violations"
    )
}

@Composable
private fun ProctorPlaceholderScreen(viewModel: UjianViewModel, title: String, subtitle: String, iconDescription: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(UjianScreen.PENGAWAS_HOME) }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (iconDescription == "Monitoring") Icons.Default.Visibility else Icons.Default.Warning,
                        contentDescription = iconDescription,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Fitur ini akan segera tersedia.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun MyApplicationThemeWrapper(content: @Composable () -> Unit) {
    com.example.ui.theme.MyApplicationTheme {
        content()
    }
}
