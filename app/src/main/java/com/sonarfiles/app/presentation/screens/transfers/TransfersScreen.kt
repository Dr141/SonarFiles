package com.sonarfiles.app.presentation.screens.transfers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonarfiles.app.core.ui.components.formatDate
import com.sonarfiles.app.core.ui.components.formatFileSize
import com.sonarfiles.app.core.ui.theme.SonarCyanPrimary
import com.sonarfiles.app.core.ui.theme.StatusError
import com.sonarfiles.app.core.ui.theme.StatusSuccess
import com.sonarfiles.app.domain.model.TransferStatus
import com.sonarfiles.app.domain.model.TransferTask
import com.sonarfiles.app.domain.model.TransferType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransfersScreen(
    viewModel: TransfersViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Transferências",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    if (uiState.completedTransfers.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearCompleted) {
                            Icon(
                                imageVector = Icons.Filled.ClearAll,
                                contentDescription = "Limpar Histórico",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = SonarCyanPrimary
                    )
                }

                uiState.isEmpty -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = SonarCyanPrimary.copy(alpha = 0.12f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Sync,
                                    contentDescription = null,
                                    tint = SonarCyanPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Nenhuma transferência recente",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Seus downloads e uploads em segundo plano aparecerão aqui.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Seção de Transferências Ativas
                        if (uiState.activeTransfers.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Em Andamento (${uiState.activeTransfers.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SonarCyanPrimary
                                )
                            }

                            items(uiState.activeTransfers, key = { it.id }) { task ->
                                ActiveTransferCard(
                                    task = task,
                                    onCancel = { viewModel.cancelTransfer(task) }
                                )
                            }
                        }

                        // Seção de Histórico Concluído
                        if (uiState.completedTransfers.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Concluídas (${uiState.completedTransfers.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            items(uiState.completedTransfers, key = { it.id }) { task ->
                                CompletedTransferCard(
                                    task = task,
                                    onDelete = { viewModel.deleteTransfer(task) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveTransferCard(
    task: TransferTask,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (task.type == TransferType.DOWNLOAD) Icons.Filled.Download else Icons.Filled.Upload,
                    contentDescription = null,
                    tint = SonarCyanPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = task.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cancelar",
                        tint = StatusError,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { task.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = SonarCyanPrimary,
                trackColor = MaterialTheme.colorScheme.background
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val transferredStr = formatFileSize(task.transferredBytes)
                val totalStr = formatFileSize(task.totalBytes)
                val percentStr = "${(task.progressFraction * 100).toInt()}%"

                Text(
                    text = "$transferredStr / $totalStr ($percentStr)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val speedStr = if (task.speedBytesPerSec > 0) "${formatFileSize(task.speedBytesPerSec)}/s" else "Calculando..."
                Text(
                    text = speedStr,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = SonarCyanPrimary
                )
            }
        }
    }
}

@Composable
fun CompletedTransferCard(
    task: TransferTask,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (statusIcon, statusTint) = when (task.status) {
                TransferStatus.COMPLETED -> Icons.Filled.CheckCircle to StatusSuccess
                TransferStatus.FAILED -> Icons.Filled.Error to StatusError
                TransferStatus.CANCELLED -> Icons.Filled.Cancel to MaterialTheme.colorScheme.onSurfaceVariant
                else -> Icons.Filled.Sync to SonarCyanPrimary
            }

            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = statusTint,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val info = if (task.status == TransferStatus.COMPLETED) {
                    "${formatFileSize(task.totalBytes)} • ${formatDate(task.completedAt ?: task.createdAt)}"
                } else {
                    task.errorMessage ?: task.status.name
                }

                Text(
                    text = info,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (task.status == TransferStatus.FAILED) StatusError else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Remover",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
