package com.sonarfiles.app.presentation.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonarfiles.app.core.ui.theme.SonarCyanPrimary
import com.sonarfiles.app.core.ui.theme.StatusError
import com.sonarfiles.app.core.ui.theme.StatusSftpBadge
import com.sonarfiles.app.core.ui.theme.StatusSmbBadge
import com.sonarfiles.app.domain.model.OsHint
import com.sonarfiles.app.domain.model.ProtocolType
import com.sonarfiles.app.domain.model.ServerConfig

data class ServerOsVisual(
    val icon: ImageVector,
    val label: String,
    val color: Color
)

fun ServerConfig.detectOs(): OsHint {
    val name = displayName.lowercase()
    return when {
        name.contains("mac") || name.contains("apple") || name.contains("osx") || name.contains("darwin") -> OsHint.MACOS
        name.contains("nas") || name.contains("synology") || name.contains("truenas") || name.contains("qnap") || name.contains("omv") -> OsHint.NAS
        name.contains("windows") || name.contains("win") || name.contains("pc") || domainOrWorkgroup != null || protocol == ProtocolType.SMB -> OsHint.WINDOWS
        name.contains("linux") || name.contains("ubuntu") || name.contains("debian") || name.contains("arch") || name.contains("fedora") || name.contains("pi") || name.contains("raspberry") || protocol == ProtocolType.SFTP -> OsHint.LINUX
        else -> if (protocol == ProtocolType.SMB) OsHint.WINDOWS else OsHint.LINUX
    }
}

fun OsHint.getVisual(): ServerOsVisual {
    return when (this) {
        OsHint.WINDOWS -> ServerOsVisual(
            icon = Icons.Filled.Computer,
            label = "Windows",
            color = Color(0xFF00A4EF)
        )
        OsHint.LINUX -> ServerOsVisual(
            icon = Icons.Filled.Terminal,
            label = "Linux",
            color = Color(0xFFFFB300)
        )
        OsHint.MACOS -> ServerOsVisual(
            icon = Icons.Filled.LaptopMac,
            label = "macOS",
            color = Color(0xFFE0E0E0)
        )
        OsHint.NAS -> ServerOsVisual(
            icon = Icons.Filled.Storage,
            label = "NAS",
            color = Color(0xFF00E676)
        )
        OsHint.UNKNOWN -> ServerOsVisual(
            icon = Icons.Filled.Dns,
            label = "Servidor",
            color = SonarCyanPrimary
        )
    }
}

/**
 * Card no formato Lista (Row expandida):
 * 1ª linha: Apenas o nome do servidor
 * 2ª linha: Badges (SO e Protocolo) + IP:Porta
 */
@Composable
fun ServerCardList(
    server: ServerConfig,
    isAccessible: Boolean? = null,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onWakeOnLan: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val osVisual = server.detectOs().getVisual()

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
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone do Sistema Operacional com Bolinha Verde/Vermelha de Acessibilidade
            Box(modifier = Modifier.size(46.dp)) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(12.dp),
                    color = osVisual.color.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, osVisual.color.copy(alpha = 0.3f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = osVisual.icon,
                            contentDescription = osVisual.label,
                            tint = osVisual.color,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Apenas a bolinha verde (acessível) ou vermelha (inacessível)
                ReachabilityStatusDot(
                    isAccessible = isAccessible,
                    size = 13.dp,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Detalhes do Servidor: 1ª linha apenas o nome, embaixo as demais informações
            Column(modifier = Modifier.weight(1f)) {
                // Primeira linha: Apenas o nome
                Text(
                    text = server.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Segunda linha: Badges (SO e Protocolo) + IP:Porta
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OsBadge(visual = osVisual)
                    ProtocolBadge(protocol = server.protocol)
                    Text(
                        text = "${server.hostOrIp}:${server.port}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!server.macAddress.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "MAC: ${server.macAddress}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Botão de Favorito
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (server.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorito",
                    tint = if (server.isFavorite) Color(0xFFFFD600) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Menu de Opções
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Mais opções",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ServerOverflowMenu(
                    expanded = menuExpanded,
                    onDismiss = { menuExpanded = false },
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onWakeOnLan = onWakeOnLan,
                    hasMacAddress = !server.macAddress.isNullOrBlank()
                )
            }
        }
    }
}

/**
 * Card no formato Grade (Grid 2 colunas)
 */
@Composable
fun ServerCardGrid(
    server: ServerConfig,
    isAccessible: Boolean? = null,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onWakeOnLan: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val osVisual = server.detectOs().getVisual()

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Linha Superior: Ícone do SO com bolinha de status à esquerda, Favorito e Menu à direita
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.size(38.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(10.dp),
                        color = osVisual.color.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, osVisual.color.copy(alpha = 0.35f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = osVisual.icon,
                                contentDescription = osVisual.label,
                                tint = osVisual.color,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    ReachabilityStatusDot(
                        isAccessible = isAccessible,
                        size = 11.dp,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = if (server.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorito",
                            tint = if (server.isFavorite) Color(0xFFFFD600) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "Opções",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        ServerOverflowMenu(
                            expanded = menuExpanded,
                            onDismiss = { menuExpanded = false },
                            onEdit = onEdit,
                            onDelete = onDelete,
                            onWakeOnLan = onWakeOnLan,
                            hasMacAddress = !server.macAddress.isNullOrBlank()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Nome do Servidor
            Text(
                text = server.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Badges: Apenas SO e Protocolo (sem texto escrito de acessível)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OsBadge(visual = osVisual)
                ProtocolBadge(protocol = server.protocol)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Host e Porta
            Text(
                text = "${server.hostOrIp}:${server.port}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Indicador de Wake-on-LAN se disponível
            if (!server.macAddress.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.PowerSettingsNew,
                        contentDescription = "Wake-on-LAN",
                        tint = SonarCyanPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "WoL ativo",
                        fontSize = 10.sp,
                        color = SonarCyanPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ReachabilityStatusDot(
    isAccessible: Boolean?,
    modifier: Modifier = Modifier,
    size: Dp = 13.dp
) {
    val dotColor = when (isAccessible) {
        true -> Color(0xFF00E676)  // Verde Acessível
        false -> Color(0xFFFF5252) // Vermelho Inacessível
        null -> Color(0xFF9E9E9E)  // Cinza Testando
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(2.dp)
            .clip(CircleShape)
            .background(dotColor)
    )
}

@Composable
fun OsBadge(visual: ServerOsVisual) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = visual.color.copy(alpha = 0.15f)
    ) {
        Text(
            text = visual.label,
            color = visual.color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
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
        color = badgeColor.copy(alpha = 0.15f)
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
private fun ServerOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onWakeOnLan: () -> Unit,
    hasMacAddress: Boolean
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
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
                onDismiss()
                onEdit()
            }
        )

        if (hasMacAddress) {
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
                    onDismiss()
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
                onDismiss()
                onDelete()
            }
        )
    }
}
