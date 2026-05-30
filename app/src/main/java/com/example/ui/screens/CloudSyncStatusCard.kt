package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.SyncStatus
import com.example.ui.theme.*
import com.example.viewmodel.UjianViewModel

@Composable
fun CloudSyncStatusCard(viewModel: UjianViewModel, modifier: Modifier = Modifier) {
    val syncStatus by viewModel.cloudSyncStatus.collectAsStateWithLifecycle()
    val syncMessage by viewModel.cloudSyncMessage.collectAsStateWithLifecycle()

    val statusColor = when (syncStatus) {
        SyncStatus.CONNECTED -> SoftGreen
        SyncStatus.SYNCING -> SmoothYellow
        SyncStatus.SYNC_SUCCESS -> SoftGreen
        SyncStatus.ERROR -> WarningRed
        SyncStatus.CONNECTING -> SmoothYellow
        else -> androidx.compose.ui.graphics.Color.Gray
    }

    val statusIcon = when (syncStatus) {
        SyncStatus.CONNECTED -> Icons.Default.Cloud
        SyncStatus.SYNCING -> Icons.Default.Refresh
        SyncStatus.SYNC_SUCCESS -> Icons.Default.Cloud
        SyncStatus.ERROR -> Icons.Default.Error
        SyncStatus.CONNECTING -> Icons.Default.Refresh
        else -> Icons.Default.Cloud
    }

    val rotation = remember { Animatable(0f) }
    
    // Animate rotation during syncing
    LaunchedEffect(syncStatus) {
        if (syncStatus == SyncStatus.SYNCING || syncStatus == SyncStatus.CONNECTING) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            rotation.snapTo(0f)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .border(
                BorderStroke(
                    1.dp,
                    statusColor.copy(alpha = 0.3f)
                ),
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = GentleSurface.copy(alpha = 0.95f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(statusColor.copy(alpha = 0.15f), CircleShape)
                    .border(BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = "Sync Icon",
                    tint = statusColor,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            if (syncStatus == SyncStatus.SYNCING || syncStatus == SyncStatus.CONNECTING) {
                                rotationZ = rotation.value
                            }
                        }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Supabase Cloud Database",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Text(
                    text = syncMessage,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = TextDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (syncStatus == SyncStatus.CONNECTED || syncStatus == SyncStatus.ERROR || syncStatus == SyncStatus.SYNC_SUCCESS || syncStatus == SyncStatus.DISCONNECTED) {
                IconButton(
                    onClick = { viewModel.startCloudSync() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Sync",
                        tint = SoftGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
