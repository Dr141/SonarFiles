package com.sonarfiles.app.presentation.screens.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonarfiles.app.R
import com.sonarfiles.app.core.ui.components.WifiRequiredDialog
import com.sonarfiles.app.core.ui.components.WifiWarningBanner
import com.sonarfiles.app.core.ui.theme.SonarCyanPrimary
import com.sonarfiles.app.core.ui.theme.StatusError
import com.sonarfiles.app.core.ui.theme.StatusSftpBadge
import com.sonarfiles.app.core.ui.theme.StatusSmbBadge
import com.sonarfiles.app.domain.model.ProtocolType
import com.sonarfiles.app.domain.model.ServerConfig
import com.sonarfiles.app.presentation.screens.home.components.ConfirmDeleteServerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSonar: () -> Unit,
    onNavigateToManualAdd: () -> Unit,
    onNavigateToEditServer: (serverId: Long) -> Unit,
    onNavigateToExplorer: (serverId: Long) -> Unit,
    onNavigateToTransfers: () -> Unit,
    onSendWakeOnLan: (mac: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var serverToDelete by remember { mutableStateOf<ServerConfig?>(null) }
    var wifiWarningDialogMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(SonarCyanPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToManualAdd) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Adicionar Servidor",
                            tint = SonarCyanPrimary
                        )
                    }
                    IconButton(onClick = onNavigateToTransfers) {
                        Icon(
                            imageVector = Icons.Filled.Sync,
                            contentDescription = "Transferências",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (viewModel.isWifiConnected()) {
                        onNavigateToSonar()
                    } else {
                        wifiWarningDialogMessage = "Para varrer a rede local com o Sonar, conecte o dispositivo à rede Wi-Fi."
                    }
                },
                containerColor = SonarCyanPrimary,
                contentColor = Color.Black,
                icon = { Icon(Icons.Filled.Radar, contentDescription = null) },
                text = { Text("Sonar Scan", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Wi-Fi Warning Banner when disconnected
            AnimatedVisibility(
                visible = !uiState.isWifiConnected,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                WifiWarningBanner(
                    message = "Wi-Fi desconectado: conecte-se à sua rede local para acessar ou escanear servidores."
                )
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

                    uiState.isEmpty -> {
                        EmptyServersView(
                            onScanClick = {
                                if (viewModel.isWifiConnected()) {
                                    onNavigateToSonar()
                                } else {
                                    wifiWarningDialogMessage = "Para varrer a rede local com o Sonar, conecte o dispositivo à rede Wi-Fi."
                                }
                            },
                            onManualAddClick = onNavigateToManualAdd,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.servers_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${uiState.servers.size} configurado(s)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            items(
                                items = uiState.servers,
                                key = { it.id }
                            ) { server ->
                                ServerCard(
                                    server = server,
                                    onClick = {
                                        if (viewModel.isWifiConnected()) {
                                            onNavigateToExplorer(server.id)
                                        } else {
                                            wifiWarningDialogMessage = "Para conectar ao servidor \"${server.displayName}\" (${server.hostOrIp}), conecte o aparelho à rede Wi-Fi local."
                                        }
                                    },
                                    onEdit = { onNavigateToEditServer(server.id) },
                                    onDelete = { serverToDelete = server },
                                    onToggleFavorite = { viewModel.toggleFavorite(server) },
                                    onWakeOnLan = {
                                        if (!server.macAddress.isNullOrBlank()) {
                                            onSendWakeOnLan(server.macAddress)
                                            Toast.makeText(context, "Pacote Wake-on-LAN enviado para ${server.displayName}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog for deleting a server
    serverToDelete?.let { server ->
        ConfirmDeleteServerDialog(
            server = server,
            onConfirm = {
                viewModel.deleteServer(server)
                serverToDelete = null
                Toast.makeText(context, "Servidor \"${server.displayName}\" excluído", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { serverToDelete = null }
        )
    }

    // Dialog when action requires active Wi-Fi
    wifiWarningDialogMessage?.let { message ->
        WifiRequiredDialog(
            message = message,
            onDismiss = { wifiWarningDialogMessage = null }
        )
    }
}

@Composable
fun ServerCard(
    server: ServerConfig,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onWakeOnLan: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Protocol Icon Avatar
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = when (server.protocol) {
                    ProtocolType.SMB -> StatusSmbBadge.copy(alpha = 0.15f)
                    ProtocolType.SFTP -> StatusSftpBadge.copy(alpha = 0.15f)
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (server.protocol == ProtocolType.SMB) Icons.Filled.FolderShared else Icons.Filled.Dns,
                        contentDescription = null,
                        tint = if (server.protocol == ProtocolType.SMB) StatusSmbBadge else StatusSftpBadge,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = server.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ProtocolBadge(protocol = server.protocol)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${server.hostOrIp}:${server.port}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!server.macAddress.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "MAC: ${server.macAddress}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Favorite Button
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (server.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorito",
                    tint = if (server.isFavorite) Color(0xFFFFD600) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Overflow Actions Menu (Edit, WoL, Delete)
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Mais opções",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    DropdownMenuItem(
                        text = { Text("Editar Configuração") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = null,
                                tint = SonarCyanPrimary
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )

                    if (!server.macAddress.isNullOrBlank()) {
                        DropdownMenuItem(
                            text = { Text("Wake-on-LAN") },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = SonarCyanPrimary
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onWakeOnLan()
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text("Excluir Configuração", color = StatusError) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = null,
                                tint = StatusError
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProtocolBadge(protocol: ProtocolType) {
    val badgeColor = when (protocol) {
        ProtocolType.SMB -> StatusSmbBadge
        ProtocolType.SFTP -> StatusSftpBadge
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = badgeColor.copy(alpha = 0.2f)
    ) {
        Text(
            text = protocol.name,
            color = badgeColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun EmptyServersView(
    onScanClick: () -> Unit,
    onManualAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = SonarCyanPrimary.copy(alpha = 0.12f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Radar,
                    contentDescription = null,
                    tint = SonarCyanPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.no_servers_found),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.no_servers_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onScanClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SonarCyanPrimary,
                contentColor = Color.Black
            )
        ) {
            Icon(Icons.Filled.Radar, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.action_scan), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onManualAddClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.action_add_manual))
        }
    }
}
