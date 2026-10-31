package com.sonarfiles.app.presentation.screens.transfers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonarfiles.app.core.ui.components.formatDate
import com.sonarfiles.app.core.ui.components.formatFileSize
import com.sonarfiles.app.core.ui.theme.SonarCyanPrimary
import com.sonarfiles.app.core.ui.theme.StatusError
import com.sonarfiles.app.core.ui.theme.StatusSuccess
import com.sonarfiles.app.domain.model.TransferStatus
import com.sonarfiles.app.domain.model.TransferTask
import com.sonarfiles.app.domain.model.TransferType
import java.io.File

private enum class TransferFilter {
    ALL, DOWNLOADS, UPLOADS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransfersScreen(
    viewModel: TransfersViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf(TransferFilter.ALL) }

    val filteredActive = remember(uiState.activeTransfers, selectedFilter) {
        when (selectedFilter) {
            TransferFilter.ALL -> uiState.activeTransfers
            TransferFilter.DOWNLOADS -> uiState.activeTransfers.filter { it.type == TransferType.DOWNLOAD }
            TransferFilter.UPLOADS -> uiState.activeTransfers.filter { it.type == TransferType.UPLOAD }
        }
    }

    val filteredCompleted = remember(uiState.completedTransfers, selectedFilter) {
        when (selectedFilter) {
            TransferFilter.ALL -> uiState.completedTransfers
            TransferFilter.DOWNLOADS -> uiState.completedTransfers.filter { it.type == TransferType.DOWNLOAD }
            TransferFilter.UPLOADS -> uiState.completedTransfers.filter { it.type == TransferType.UPLOAD }
        }
    }

    val totalCount = uiState.activeTransfers.size + uiState.completedTransfers.size
    val downloadsCount = (uiState.activeTransfers + uiState.completedTransfers).count { it.type == TransferType.DOWNLOAD }
    val uploadsCount = (uiState.activeTransfers + uiState.completedTransfers).count { it.type == TransferType.UPLOAD }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filtros: Todos, Downloads, Uploads
            if (!uiState.isLoading && !uiState.isEmpty) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == TransferFilter.ALL,
                        onClick = { selectedFilter = TransferFilter.ALL },
                        label = { Text("Todos ($totalCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SonarCyanPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = SonarCyanPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedFilter == TransferFilter.ALL,
                            selectedBorderColor = SonarCyanPrimary
                        )
                    )

                    FilterChip(
                        selected = selectedFilter == TransferFilter.DOWNLOADS,
                        onClick = { selectedFilter = TransferFilter.DOWNLOADS },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Downloads ($downloadsCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SonarCyanPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = SonarCyanPrimary,
                            selectedLeadingIconColor = SonarCyanPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedFilter == TransferFilter.DOWNLOADS,
                            selectedBorderColor = SonarCyanPrimary
                        )
                    )

                    FilterChip(
                        selected = selectedFilter == TransferFilter.UPLOADS,
                        onClick = { selectedFilter = TransferFilter.UPLOADS },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Upload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Uploads ($uploadsCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SonarCyanPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = SonarCyanPrimary,
                            selectedLeadingIconColor = SonarCyanPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedFilter == TransferFilter.UPLOADS,
                            selectedBorderColor = SonarCyanPrimary
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = SonarCyanPrimary
                        )
                    }

                    filteredActive.isEmpty() && filteredCompleted.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                modifier = Modifier.size(72.dp),
                                shape = CircleShape,
                                color = SonarCyanPrimary.copy(alpha = 0.12f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Sync,
                                        contentDescription = null,
                                        tint = SonarCyanPrimary,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "Nenhuma transferência recente",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Seus downloads e uploads realizados aparecerão listados aqui com detalhes e progresso.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Seção de Transferências Ativas
                            if (filteredActive.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Em Andamento (${filteredActive.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SonarCyanPrimary
                                    )
                                }

                                items(filteredActive, key = { it.id }) { task ->
                                    ActiveTransferCard(
                                        task = task,
                                        onCancel = { viewModel.cancelTransfer(task) }
                                    )
                                }
                            }

                            // Seção de Histórico Concluído
                            if (filteredCompleted.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Histórico (${filteredCompleted.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = if (filteredActive.isNotEmpty()) 8.dp else 0.dp)
                                    )
                                }

                                items(filteredCompleted, key = { it.id }) { task ->
                                    CompletedTransferCard(
                                        task = task,
                                        onOpen = { openDownloadedTask(context, task) },
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
}

private fun openDownloadedTask(context: Context, task: TransferTask) {
    if (task.type != TransferType.DOWNLOAD || task.status != TransferStatus.COMPLETED) return
    try {
        val ext = MimeTypeMap.getFileExtensionFromUrl(task.fileName).lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"

        val uri: Uri = if (task.localUriOrPath.startsWith("content://")) {
            Uri.parse(task.localUriOrPath)
        } else {
            val file = File(task.localUriOrPath)
            if (!file.exists()) {
                Toast.makeText(context, "Arquivo local não encontrado", Toast.LENGTH_SHORT).show()
                return
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Abrir com..."))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Nenhum aplicativo encontrado para abrir este arquivo", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Não foi possível abrir o arquivo", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ActiveTransferCard(
    task: TransferTask,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDownload = task.type == TransferType.DOWNLOAD
    val percent = (task.progressFraction * 100).toInt()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, SonarCyanPrimary.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = SonarCyanPrimary.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isDownload) Icons.Filled.Download else Icons.Filled.Upload,
                            contentDescription = null,
                            tint = SonarCyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // 1ª linha: Apenas o nome do arquivo
                    Text(
                        text = task.fileName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 2ª linha: Tipo + Servidor
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TransferTypeBadge(isDownload = isDownload)
                        if (task.serverDisplayName.isNotBlank()) {
                            Text(
                                text = "• ${task.serverDisplayName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SonarCyanPrimary,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
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

                Text(
                    text = "$transferredStr de $totalStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val speedStr = if (task.speedBytesPerSec > 0) "${formatFileSize(task.speedBytesPerSec)}/s" else "Transferindo..."
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
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDownload = task.type == TransferType.DOWNLOAD
    val canOpen = isDownload && task.status == TransferStatus.COMPLETED

    val (statusIcon, statusTint, statusLabel) = when (task.status) {
        TransferStatus.COMPLETED -> Triple(Icons.Filled.CheckCircle, StatusSuccess, "Concluído")
        TransferStatus.FAILED -> Triple(Icons.Filled.Error, StatusError, "Falhou")
        TransferStatus.CANCELLED -> Triple(Icons.Filled.Cancel, MaterialTheme.colorScheme.onSurfaceVariant, "Cancelado")
        else -> Triple(Icons.Filled.Sync, SonarCyanPrimary, "Em fila")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(if (canOpen) Modifier.clickable(onClick = onOpen) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone indicando Download ou Upload com cor do status
            Box(modifier = Modifier.size(42.dp)) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(10.dp),
                    color = statusTint.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isDownload) Icons.Filled.Download else Icons.Filled.Upload,
                            contentDescription = null,
                            tint = statusTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Icon(
                    imageVector = statusIcon,
                    contentDescription = statusLabel,
                    tint = statusTint,
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.BottomEnd)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 1ª linha: Apenas o nome do arquivo
                Text(
                    text = task.fileName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(5.dp))

                // 2ª linha: Badges (Download/Upload + Status + Servidor)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TransferTypeBadge(isDownload = isDownload)

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusTint.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = statusLabel,
                            color = statusTint,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    if (task.serverDisplayName.isNotBlank()) {
                        Text(
                            text = task.serverDisplayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 3ª linha: Tamanho e Data ou Mensagem de erro
                val info = if (task.status == TransferStatus.COMPLETED) {
                    "${formatFileSize(task.totalBytes)} • ${formatDate(task.completedAt ?: task.createdAt)}"
                } else {
                    task.errorMessage ?: statusLabel
                }

                Text(
                    text = info,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (task.status == TransferStatus.FAILED) StatusError else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (canOpen) {
                IconButton(onClick = onOpen, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.OpenInNew,
                        contentDescription = "Abrir arquivo",
                        tint = SonarCyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Remover",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TransferTypeBadge(isDownload: Boolean) {
    val badgeColor = if (isDownload) SonarCyanPrimary else Color(0xFFAB47BC)
    val label = if (isDownload) "Download" else "Upload"

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = badgeColor.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            color = badgeColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}
