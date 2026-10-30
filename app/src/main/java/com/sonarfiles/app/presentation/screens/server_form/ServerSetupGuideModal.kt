package com.sonarfiles.app.presentation.screens.server_form

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonarfiles.app.core.ui.theme.SonarCyanPrimary
import com.sonarfiles.app.core.ui.theme.StatusSuccess
import com.sonarfiles.app.domain.model.ProtocolType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSetupGuideModal(
    initialProtocol: ProtocolType = ProtocolType.SMB,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        ServerSetupGuideContent(
            initialProtocol = initialProtocol,
            onClose = onDismiss
        )
    }
}

/**
 * Alias de compatibilidade
 */
@Composable
fun WindowsSmbSetupGuideModal(
    onDismiss: () -> Unit
) {
    ServerSetupGuideModal(
        initialProtocol = ProtocolType.SMB,
        onDismiss = onDismiss
    )
}

@Composable
fun ServerSetupGuideContent(
    initialProtocol: ProtocolType,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedProtocol by remember(initialProtocol) { mutableStateOf(initialProtocol) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabeçalho do Modal
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = (if (selectedProtocol == ProtocolType.SMB) Color(0xFF00A4EF) else Color(0xFFFFB300)).copy(alpha = 0.15f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (selectedProtocol == ProtocolType.SMB) Icons.Filled.Computer else Icons.Filled.Terminal,
                            contentDescription = null,
                            tint = if (selectedProtocol == ProtocolType.SMB) Color(0xFF00A4EF) else Color(0xFFFFB300),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (selectedProtocol == ProtocolType.SMB) "Guia Windows (SMB)" else "Guia Linux (SFTP / SSH)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Passo a passo de liberação e conexão",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Fechar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Seletor de Sistema Operacional (Tabs)
        TabRow(
            selectedTabIndex = if (selectedProtocol == ProtocolType.SMB) 0 else 1,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = SonarCyanPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[if (selectedProtocol == ProtocolType.SMB) 0 else 1]),
                    color = SonarCyanPrimary
                )
            }
        ) {
            Tab(
                selected = selectedProtocol == ProtocolType.SMB,
                onClick = { selectedProtocol = ProtocolType.SMB },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Computer, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Windows (SMB)", fontWeight = FontWeight.Bold)
                    }
                }
            )
            Tab(
                selected = selectedProtocol == ProtocolType.SFTP,
                onClick = { selectedProtocol = ProtocolType.SFTP },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Linux (SSH/SFTP)", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Conteúdo conforme o SO selecionado
        if (selectedProtocol == ProtocolType.SMB) {
            WindowsSmbGuideSection()
        } else {
            LinuxSftpGuideSection()
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Botão Fechar Guia
        Button(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SonarCyanPrimary,
                contentColor = Color.Black
            )
        ) {
            Text("Entendi, Fechar Guia", fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Seção de passos para Windows (SMB)
 */
@Composable
private fun WindowsSmbGuideSection() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "💡 Por que configurar no Windows?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF00A4EF)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "O Windows bloqueia o compartilhamento quando a rede Wi-Fi está definida como pública ou quando a conta usa e-mail Microsoft com PIN do Windows Hello (Erro 1326). Siga os passos:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        // Passo 1: Rede Privada
        GuideStepCard(
            stepNumber = "1",
            title = "Mudar o Wi-Fi para Rede Privada",
            icon = Icons.Filled.Wifi,
            accentColor = Color(0xFF00A4EF)
        ) {
            Text(
                text = "O Firewall do Windows descarta a porta 445 (SMB) se a rede estiver em modo Público.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            BulletItem(text = "No Windows, abra Configurações > Rede e Internet > Wi-Fi.")
            BulletItem(text = "Clique na sua rede Wi-Fi conectada.")
            BulletItem(text = "Em Tipo de perfil de rede, alterne para Rede privada.")
        }

        // Passo 2: Usuário Local
        GuideStepCard(
            stepNumber = "2",
            title = "Criar Usuário Local Dedicado",
            icon = Icons.Filled.PersonAdd,
            accentColor = Color(0xFF00A4EF)
        ) {
            Text(
                text = "Contas Microsoft com PIN do Windows Hello bloqueiam o login SMB. Criar um usuário local é a solução oficial da Microsoft e funciona de primeira.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Execute este comando no Prompt de Comando (CMD) como Administrador:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            CopyableCommandBox(command = "net user sonar Senha123! /add")

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "💡 Você pode trocar 'sonar' e 'Senha123!' pelo nome e senha que desejar.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Passo 3: Permissões de Compartilhamento
        GuideStepCard(
            stepNumber = "3",
            title = "Permissões na Pasta Compartilhada",
            icon = Icons.Filled.FolderShared,
            accentColor = Color(0xFF00A4EF)
        ) {
            Text(
                text = "Libere o acesso da pasta para o usuário local criado:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            BulletItem(text = "Clique com botão direito na pasta > Propriedades.")
            BulletItem(text = "Aba Compartilhamento > Compartilhamento Avançado > marque Compartilhar esta pasta.")
            BulletItem(text = "Clique em Permissões > Adicionar > digite sonar > dê OK e marque Controle Total.")
            BulletItem(text = "Aba Segurança da pasta > Editar > Adicionar > digite sonar > marque Modificar.")
        }

        // Passo 4: Preenchimento
        GuideStepCard(
            stepNumber = "4",
            title = "Conectar no SonarFiles",
            icon = Icons.Filled.CheckCircle,
            accentColor = Color(0xFF00A4EF)
        ) {
            BulletItem(text = "IP ou Hostname: O IPv4 da sua máquina (veja rodando ipconfig no CMD).")
            BulletItem(text = "Porta: 445 (padrão SMB).")
            BulletItem(text = "Compartilhamento: O nome da pasta compartilhada.")
            BulletItem(text = "Usuário e Senha: O usuário local (ex: sonar) e senha definidos.")
            BulletItem(text = "Domínio: Deixe em branco.")
        }
    }
}

/**
 * Seção de passos para Linux (SFTP / SSH)
 */
@Composable
private fun LinuxSftpGuideSection() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "🐧 Por que configurar no Linux?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFFFFB300)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "O SonarFiles conecta ao Linux através do protocolo SFTP (sobre SSH, porta 22). Para conectar, basta que o serviço OpenSSH Server esteja instalado e ativo no seu Linux.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        // Passo 1: Instalar OpenSSH Server
        GuideStepCard(
            stepNumber = "1",
            title = "Instalar e Ativar o OpenSSH Server",
            icon = Icons.Filled.Terminal,
            accentColor = Color(0xFFFFB300)
        ) {
            Text(
                text = "Abra o terminal no Linux e execute o comando correspondente à sua distribuição:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Ubuntu / Debian / Raspberry Pi / Mint:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            CopyableCommandBox(command = "sudo apt update && sudo apt install -y openssh-server && sudo systemctl enable --now ssh")

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Fedora / RHEL / CentOS:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            CopyableCommandBox(command = "sudo dnf install -y openssh-server && sudo systemctl enable --now sshd")

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Arch Linux / Manjaro:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            CopyableCommandBox(command = "sudo pacman -S --noconfirm openssh && sudo systemctl enable --now sshd")
        }

        // Passo 2: Firewall
        GuideStepCard(
            stepNumber = "2",
            title = "Liberar a Porta 22 no Firewall do Linux",
            icon = Icons.Filled.Security,
            accentColor = Color(0xFFFFB300)
        ) {
            Text(
                text = "Se o seu Linux utilizar firewall ativo (UFW ou Firewalld), libere a porta 22:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "No Ubuntu / Debian (UFW):",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            CopyableCommandBox(command = "sudo ufw allow ssh")

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No Fedora / RHEL (Firewalld):",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            CopyableCommandBox(command = "sudo firewall-cmd --permanent --add-service=ssh && sudo firewall-cmd --reload")
        }

        // Passo 3: Descobrir IP e Usuário
        GuideStepCard(
            stepNumber = "3",
            title = "Identificar o IP e Usuário do Linux",
            icon = Icons.Filled.Wifi,
            accentColor = Color(0xFFFFB300)
        ) {
            Text(
                text = "No terminal do seu Linux, verifique as informações de conexão:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            BulletItem(text = "Para ver o IP local: execute 'hostname -I' ou 'ip a' (pegue o primeiro IP, ex: 192.168.1.X).")
            BulletItem(text = "Para ver seu usuário atual: execute 'whoami'.")
        }

        // Passo 4: Conectar
        GuideStepCard(
            stepNumber = "4",
            title = "Preencher no SonarFiles",
            icon = Icons.Filled.CheckCircle,
            accentColor = Color(0xFFFFB300)
        ) {
            BulletItem(text = "Protocolo: Selecione SFTP (Linux/SSH).")
            BulletItem(text = "IP ou Hostname: O IP do seu Linux (ex: 192.168.1.50).")
            BulletItem(text = "Porta: 22 (padrão SSH).")
            BulletItem(text = "Diretório Inicial: /home/seu_usuario ou / para ver a raiz.")
            BulletItem(text = "Usuário e Senha: O seu usuário do Linux e a respectiva senha.")
            BulletItem(text = "Domínio: Deixe em branco.")
        }
    }
}

@Composable
private fun CopyableCommandBox(command: String) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Black.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, SonarCyanPrimary.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                clipboardManager.setText(AnnotatedString(command))
                copied = true
                Toast.makeText(context, "Comando copiado!", Toast.LENGTH_SHORT).show()
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = command,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = SonarCyanPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                    contentDescription = "Copiar",
                    tint = if (copied) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (copied) "Copiado!" else "Copiar",
                    fontSize = 11.sp,
                    color = if (copied) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GuideStepCard(
    stepNumber: String,
    title: String,
    icon: ImageVector,
    accentColor: Color = SonarCyanPrimary,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentColor,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stepNumber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            content()
        }
    }
}

@Composable
private fun BulletItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            color = SonarCyanPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 17.sp
        )
    }
}
