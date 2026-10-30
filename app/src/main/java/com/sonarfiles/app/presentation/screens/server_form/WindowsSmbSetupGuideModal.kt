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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WindowsSmbSetupGuideModal(
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        WindowsSmbSetupGuideContent(onClose = onDismiss)
    }
}

@Composable
fun WindowsSmbSetupGuideContent(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var copiedCommand by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabeçalho
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
                    color = SonarCyanPrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Computer,
                            contentDescription = null,
                            tint = SonarCyanPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Configuração do Windows",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Passo a passo para liberar o compartilhamento SMB",
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

        // Introdução explicativa
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "💡 Por que configurar?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = SonarCyanPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "O Windows bloqueia o acesso via rede quando a conexão Wi-Fi está definida como pública ou quando o usuário utiliza login via Conta Microsoft / PIN do Windows Hello. Siga os 3 passos abaixo:",
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
            icon = Icons.Filled.Wifi
        ) {
            Text(
                text = "O Firewall do Windows bloqueia a porta 445 (SMB) se a rede estiver em modo Público.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            BulletItem(text = "No Windows, abra Configurações > Rede e Internet > Wi-Fi.")
            BulletItem(text = "Clique na rede conectada ao seu computador.")
            BulletItem(text = "Em Tipo de perfil de rede, selecione Rede privada.")
        }

        // Passo 2: Usuário Local
        GuideStepCard(
            stepNumber = "2",
            title = "Criar Usuário Local Dedicado",
            icon = Icons.Filled.PersonAdd
        ) {
            Text(
                text = "Contas vinculadas à Microsoft (@hotmail/@outlook) com PIN do Windows Hello bloqueiam o protocolo SMB (Erro 1326). Criar um usuário local é a solução oficial da Microsoft e funciona de primeira.",
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

            // Box do Comando CMD com botão de copiar
            val cmdCommand = "net user sonar Senha123! /add"
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, SonarCyanPrimary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        clipboardManager.setText(AnnotatedString(cmdCommand))
                        copiedCommand = true
                        Toast
                            .makeText(context, "Comando copiado!", Toast.LENGTH_SHORT)
                            .show()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = cmdCommand,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = SonarCyanPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (copiedCommand) Icons.Filled.Check else Icons.Filled.ContentCopy,
                            contentDescription = "Copiar comando",
                            tint = if (copiedCommand) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (copiedCommand) "Copiado!" else "Copiar",
                            fontSize = 11.sp,
                            color = if (copiedCommand) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

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
            icon = Icons.Filled.FolderShared
        ) {
            Text(
                text = "Libere o acesso da pasta para o usuário local que você acabou de criar:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            BulletItem(text = "Clique com botão direito na pasta no Windows > Propriedades.")
            BulletItem(text = "Aba Compartilhamento > Compartilhamento Avançado > marque Compartilhar esta pasta.")
            BulletItem(text = "Clique no botão Permissões > Adicionar > digite sonar > dê OK e marque Controle Total.")
            BulletItem(text = "Aba Segurança da pasta > Editar > Adicionar > digite sonar > dê OK e marque Modificar.")
        }

        // Passo 4: Conectar no SonarFiles
        GuideStepCard(
            stepNumber = "4",
            title = "Conectar no SonarFiles",
            icon = Icons.Filled.CheckCircle
        ) {
            BulletItem(text = "IP ou Hostname: Digite o IP da máquina (veja rodando ipconfig no CMD).")
            BulletItem(text = "Porta: 445 (padrão SMB).")
            BulletItem(text = "Compartilhamento: Nome da pasta compartilhada no Windows.")
            BulletItem(text = "Usuário e Senha: O usuário criado (ex: sonar) e a senha escolhida.")
            BulletItem(text = "Domínio: Deixe em branco.")
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

@Composable
private fun GuideStepCard(
    stepNumber: String,
    title: String,
    icon: ImageVector,
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
                    color = SonarCyanPrimary,
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
                    tint = SonarCyanPrimary,
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
