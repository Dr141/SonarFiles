package com.sonarfiles.app.presentation.screens.server_form

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonarfiles.app.core.ui.theme.SonarCyanPrimary
import com.sonarfiles.app.core.ui.theme.StatusError
import com.sonarfiles.app.core.ui.theme.StatusSuccess
import com.sonarfiles.app.domain.model.DiagnosticStepResult
import com.sonarfiles.app.domain.model.ProtocolType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerFormScreen(
    viewModel: ServerFormViewModel,
    onNavigateBack: () -> Unit,
    onSavedSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var passwordVisible by remember { mutableStateOf(false) }
    var showDiagnosticModal by remember { mutableStateOf(false) }
    var showHelpGuide by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) {
            onSavedSuccess()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (uiState.serverId == null) "Novo Servidor" else "Editar Servidor",
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
                    IconButton(onClick = { showHelpGuide = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Ajuda e Configuração",
                            tint = SonarCyanPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    if (!uiState.errorMessage.isNullOrBlank()) {
                        Text(
                            text = uiState.errorMessage!!,
                            color = StatusError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (uiState.hostOrIp.isNotBlank()) {
                                    showDiagnosticModal = true
                                }
                                viewModel.testConnection()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !uiState.isTestingConnection
                        ) {
                            Icon(Icons.Filled.NetworkCheck, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Testar")
                        }

                        Button(
                            onClick = viewModel::saveServer,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SonarCyanPrimary,
                                contentColor = Color.Black
                            ),
                            enabled = !uiState.isSaving
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.Black
                                )
                            } else {
                                Icon(Icons.Filled.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Salvar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Seletor de Protocolo (Tabs)
            TabRow(
                selectedTabIndex = if (uiState.protocol == ProtocolType.SMB) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = SonarCyanPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[if (uiState.protocol == ProtocolType.SMB) 0 else 1]),
                        color = SonarCyanPrimary
                    )
                }
            ) {
                Tab(
                    selected = uiState.protocol == ProtocolType.SMB,
                    onClick = { viewModel.onProtocolChanged(ProtocolType.SMB) },
                    text = { Text("SMB (Windows/Samba)", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = uiState.protocol == ProtocolType.SFTP,
                    onClick = { viewModel.onProtocolChanged(ProtocolType.SFTP) },
                    text = { Text("SFTP (Linux/SSH)", fontWeight = FontWeight.Bold) }
                )
            }

            // Banner de Ajuda Rápida do Windows (para SMB)
            if (uiState.protocol == ProtocolType.SMB) {
                Surface(
                    onClick = { showHelpGuide = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, SonarCyanPrimary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            tint = SonarCyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Como configurar o Windows?",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Toque para ver o passo a passo: rede privada, usuário local e permissões",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = SonarCyanPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Nome de exibição
            OutlinedTextField(
                value = uiState.displayName,
                onValueChange = viewModel::onDisplayNameChanged,
                label = { Text("Nome de Exibição") },
                placeholder = { Text("Ex: PC do Quarto, Home NAS") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = outlinedColors()
            )

            // IP e Porta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.hostOrIp,
                    onValueChange = viewModel::onHostOrIpChanged,
                    label = { Text("IP ou Hostname *") },
                    placeholder = { Text("192.168.1.100") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = outlinedColors()
                )

                OutlinedTextField(
                    value = uiState.port,
                    onValueChange = viewModel::onPortChanged,
                    label = { Text("Porta") },
                    modifier = Modifier.width(100.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = outlinedColors()
                )
            }

            // Caminho / Compartilhamento padrão
            OutlinedTextField(
                value = uiState.defaultShareOrPath,
                onValueChange = viewModel::onDefaultPathChanged,
                label = { Text(if (uiState.protocol == ProtocolType.SMB) "Compartilhamento Inicial" else "Diretório Inicial") },
                placeholder = { Text(if (uiState.protocol == ProtocolType.SMB) "Ex: Users ou C$" else "/home/user") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = outlinedColors()
            )

            // Usuário e Senha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.username,
                    onValueChange = viewModel::onUsernameChanged,
                    label = { Text("Usuário") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = outlinedColors()
                )

                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = viewModel::onPasswordChanged,
                    label = { Text("Senha") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = outlinedColors()
                )
            }

            // Domínio (apenas SMB)
            if (uiState.protocol == ProtocolType.SMB) {
                OutlinedTextField(
                    value = uiState.domainOrWorkgroup,
                    onValueChange = viewModel::onDomainChanged,
                    label = { Text("Domínio / Workgroup (Opcional)") },
                    placeholder = { Text("WORKGROUP") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = outlinedColors()
                )
            }

            // MAC Address para Wake-on-LAN
            OutlinedTextField(
                value = uiState.macAddress,
                onValueChange = viewModel::onMacAddressChanged,
                label = { Text("Endereço MAC (Para Wake-on-LAN)") },
                placeholder = { Text("00:1A:2B:3C:4D:5E") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = outlinedColors()
            )

            // Switch Biometria
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = SonarCyanPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Exigir Biometria",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Desbloqueie com impressão digital ou PIN para conectar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.requiresBiometrics,
                        onCheckedChange = viewModel::onRequiresBiometricsChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = SonarCyanPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Modal de Diagnóstico da Conexão
    if (showDiagnosticModal) {
        ModalBottomSheet(
            onDismissRequest = { showDiagnosticModal = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            DiagnosticBottomSheetContent(
                uiState = uiState,
                onRetest = viewModel::testConnection,
                onOpenHelpGuide = {
                    showHelpGuide = true
                },
                onSaveAndClose = {
                    showDiagnosticModal = false
                    viewModel.saveServer()
                },
                onClose = { showDiagnosticModal = false }
            )
        }
    }

    // Modal do Guia de Configuração SMB (Windows)
    if (showHelpGuide) {
        WindowsSmbSetupGuideModal(onDismiss = { showHelpGuide = false })
    }
}

@Composable
fun DiagnosticBottomSheetContent(
    uiState: ServerFormUiState,
    onRetest: () -> Unit,
    onOpenHelpGuide: () -> Unit,
    onSaveAndClose: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allPassed = uiState.diagnosticResults.values.all { it is StepStatus.Passed }
    val anyFailed = uiState.diagnosticResults.values.any { it is StepStatus.Failed }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Cabeçalho do Modal
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Diagnóstico da Conexão",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${if (uiState.protocol == ProtocolType.SMB) "SMB" else "SFTP"} • ${uiState.hostOrIp}:${uiState.port}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Fechar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card com as 4 Etapas
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                DiagnosticStepRow(
                    title = "1. Alcance do Host (IP/DNS)",
                    status = uiState.diagnosticResults[DiagnosticStepResult.Step.HOST_REACHABILITY] ?: StepStatus.Pending
                )
                DiagnosticStepRow(
                    title = "2. Porta Aberta (${uiState.port})",
                    status = uiState.diagnosticResults[DiagnosticStepResult.Step.PORT_OPEN] ?: StepStatus.Pending
                )
                DiagnosticStepRow(
                    title = "3. Autenticação",
                    status = uiState.diagnosticResults[DiagnosticStepResult.Step.AUTHENTICATION] ?: StepStatus.Pending
                )
                DiagnosticStepRow(
                    title = "4. Acesso ao Diretório Raiz",
                    status = uiState.diagnosticResults[DiagnosticStepResult.Step.ROOT_ACCESS] ?: StepStatus.Pending
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status Geral / Resumo
        when {
            uiState.isTestingConnection -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = SonarCyanPrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = SonarCyanPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Validando etapas de conexão...",
                        fontSize = 12.sp,
                        color = SonarCyanPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            allPassed -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = StatusSuccess.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = StatusSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sucesso! Servidor pronto para conexão.",
                        fontSize = 12.sp,
                        color = StatusSuccess,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            anyFailed -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = StatusError.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = null,
                        tint = StatusError,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Falha na conexão. Siga as orientações acima.",
                        fontSize = 12.sp,
                        color = StatusError,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Atalho para o Guia de Configuração do Windows
        if (uiState.protocol == ProtocolType.SMB) {
            Surface(
                onClick = onOpenHelpGuide,
                shape = RoundedCornerShape(10.dp),
                color = SonarCyanPrimary.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, SonarCyanPrimary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = null,
                        tint = SonarCyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Como configurar o Windows? Ver Passo a Passo",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SonarCyanPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = SonarCyanPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Botões no Rodapé do Modal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRetest,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isTestingConnection
            ) {
                Icon(Icons.Filled.NetworkCheck, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Repetir Teste")
            }

            if (allPassed) {
                Button(
                    onClick = onSaveAndClose,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SonarCyanPrimary,
                        contentColor = Color.Black
                    ),
                    enabled = !uiState.isSaving
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Salvar", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Fechar")
                }
            }
        }
    }
}

@Composable
fun DiagnosticStepRow(
    title: String,
    status: StepStatus
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (status) {
                is StepStatus.Pending -> {
                    Icon(
                        imageVector = Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                is StepStatus.InProgress -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = SonarCyanPrimary
                    )
                }
                is StepStatus.Passed -> {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = StatusSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                }
                is StepStatus.Failed -> {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = null,
                        tint = StatusError,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                when (status) {
                    is StepStatus.Passed -> {
                        val detail = if (status.details != null) " (${status.details})" else ""
                        Text(
                            text = "Sucesso • ${status.latencyMs} ms$detail",
                            fontSize = 11.sp,
                            color = StatusSuccess
                        )
                    }
                    is StepStatus.Failed -> {
                        Text(
                            text = status.reason,
                            fontSize = 12.sp,
                            color = StatusError,
                            lineHeight = 16.sp
                        )
                    }
                    else -> {}
                }
            }
        }

        if (status is StepStatus.Failed && !status.hint.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = SonarCyanPrimary.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = SonarCyanPrimary,
                    modifier = Modifier
                        .size(15.dp)
                        .padding(top = 1.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = status.hint,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
fun outlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SonarCyanPrimary,
    focusedLabelColor = SonarCyanPrimary,
    cursorColor = SonarCyanPrimary
)
