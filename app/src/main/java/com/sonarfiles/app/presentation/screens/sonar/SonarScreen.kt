package com.sonarfiles.app.presentation.screens.sonar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonarfiles.app.core.ui.components.RadarPulseAnimation
import com.sonarfiles.app.core.ui.theme.SonarCyanPrimary
import com.sonarfiles.app.core.ui.theme.StatusSftpBadge
import com.sonarfiles.app.core.ui.theme.StatusSmbBadge
import com.sonarfiles.app.core.ui.theme.StatusSuccess
import com.sonarfiles.app.domain.model.DiscoveredHost
import com.sonarfiles.app.domain.model.OsHint
import com.sonarfiles.app.domain.model.ProtocolType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SonarScreen(
    viewModel: SonarViewModel,
    onNavigateBack: () -> Unit,
    onSelectHost: (host: DiscoveredHost, protocol: ProtocolType) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Sonar Network",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (!uiState.subnetPrefix.isNullOrBlank()) {
                            Text(
                                text = "Sub-rede: ${uiState.subnetPrefix}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SonarCyanPrimary
                            )
                        }
                    }
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
                    IconButton(
                        onClick = {
                            if (uiState.isScanning) viewModel.stopScan() else viewModel.startScan()
                        }
                    ) {
                        Icon(
                            imageVector = if (uiState.isScanning) Icons.Filled.Stop else Icons.Filled.Refresh,
                            contentDescription = if (uiState.isScanning) "Parar" else "Reescanear",
                            tint = if (uiState.isScanning) Color(0xFFFF5252) else SonarCyanPrimary
                        )
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
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Seção Superior: Radar Sonar Animado
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                RadarPulseAnimation(
                    isScanning = uiState.isScanning,
                    radarSize = 220.dp,
                    discoveredHostCount = uiState.totalFound
                )

                // Status Badge Central/Inferior
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(top = 180.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = SonarCyanPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Varrendo sub-rede...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "${uiState.totalFound} dispositivos encontrados",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SonarCyanPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Seção Inferior: Lista em Tempo Real de Hosts Descobertos
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = uiState.hosts,
                    key = { it.ipAddress }
                ) { host ->
                    DiscoveredHostCard(
                        host = host,
                        onConnectClick = { protocol ->
                            onSelectHost(host, protocol)
                        }
                    )
                }

                if (uiState.hosts.isEmpty() && !uiState.isScanning) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhum servidor SMB ou SFTP detectado na sub-rede.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoveredHostCard(
    host: DiscoveredHost,
    onConnectClick: (ProtocolType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone do SO
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = SonarCyanPrimary.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (host.osHint) {
                            OsHint.WINDOWS -> Icons.Filled.Computer
                            OsHint.LINUX -> Icons.Filled.Terminal
                            else -> Icons.Filled.Dns
                        },
                        contentDescription = null,
                        tint = SonarCyanPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Informações do Host
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = host.hostname ?: host.ipAddress,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (host.hostname != null) {
                    Text(
                        text = host.ipAddress,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Badges de Protocolo e Latência
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    host.supportedProtocols.forEach { protocol ->
                        ProtocolChip(protocol = protocol)
                    }

                    // Chip de Latência
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "${host.responseTimeMs} ms",
                            fontSize = 10.sp,
                            color = StatusSuccess,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Botão de Conexão Rápida
            val preferredProtocol = host.supportedProtocols.firstOrNull() ?: ProtocolType.SMB
            Button(
                onClick = { onConnectClick(preferredProtocol) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SonarCyanPrimary,
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Conectar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProtocolChip(protocol: ProtocolType) {
    val (label, color) = when (protocol) {
        ProtocolType.SMB -> "SMB (445)" to StatusSmbBadge
        ProtocolType.SFTP -> "SFTP (22)" to StatusSftpBadge
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.18f)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
