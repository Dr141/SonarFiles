package com.sonarfiles.app.presentation.screens.explorer

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonarfiles.app.core.ui.components.BreadcrumbBar
import com.sonarfiles.app.core.ui.components.FileItemGrid
import com.sonarfiles.app.core.ui.components.FileItemRow
import com.sonarfiles.app.core.ui.components.formatDate
import com.sonarfiles.app.core.ui.components.formatFileSize
import com.sonarfiles.app.core.ui.components.getFileIconAndTint
import com.sonarfiles.app.core.ui.theme.SonarCyanPrimary
import com.sonarfiles.app.core.ui.theme.StatusError
import com.sonarfiles.app.domain.model.RemoteFile
import com.sonarfiles.app.domain.model.TransferTask
import com.sonarfiles.app.domain.model.TransferType
import com.sonarfiles.app.domain.usecase.explorer.SortMode
import com.sonarfiles.app.presentation.screens.server_form.outlinedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(
    viewModel: ExplorerViewModel,
    serverId: Long,
    initialPath: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }
    var fileToSave by remember { mutableStateOf<RemoteFile?>(null) }

    // Launcher do SAF para escolher onde salvar 1 arquivo no telefone
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { destinationUri ->
        if (destinationUri != null && fileToSave != null) {
            viewModel.downloadFileToUri(fileToSave!!, destinationUri.toString())
            fileToSave = null
        }
    }

    // Launcher do SAF para escolher uma pasta de destino ao baixar múltiplos arquivos
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            viewModel.downloadSelectedFilesToTreeUri(treeUri)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // Launcher para escolher arquivos locais e enviar para o servidor
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.uploadFilesFromUris(uris, context.contentResolver)
        }
    }

    // Observa evento de abertura de arquivo
    LaunchedEffect(Unit) {
        viewModel.openFileEvent.collect { event ->
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                event.file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, event.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(Intent.createChooser(intent, "Abrir com..."))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "Nenhum aplicativo encontrado para abrir este arquivo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Exibe mensagens informativas em Snackbar
    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearInfoMessage()
        }
    }

    // Inicializa a sessão autenticada com o servidor
    LaunchedEffect(serverId) {
        val activity = context as? FragmentActivity
        if (activity != null) {
            viewModel.initSession(activity, serverId, initialPath)
        }
    }

    // Intercepta botão voltar nativo para navegar na árvore de diretórios
    BackHandler {
        val navigatedUp = viewModel.navigateUp()
        if (!navigatedUp) {
            onNavigateBack()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (uiState.isSelectionMode) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "${uiState.selectedItems.size} selecionado(s)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val totalSelectedSize = uiState.selectedFiles.sumOf { it.sizeBytes }
                            Text(
                                text = formatFileSize(totalSelectedSize),
                                style = MaterialTheme.typography.labelSmall,
                                color = SonarCyanPrimary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Cancelar Seleção",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    actions = {
                        if (uiState.selectedItems.isNotEmpty()) {
                            IconButton(onClick = { viewModel.openConfirmBatchDeleteDialog(true) }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Apagar Selecionados",
                                    tint = StatusError
                                )
                            }
                        }
                        IconButton(onClick = viewModel::selectAllFiles) {
                            Icon(
                                imageVector = Icons.Filled.SelectAll,
                                contentDescription = "Selecionar Todos",
                                tint = SonarCyanPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        if (uiState.isSearchActive) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = viewModel::setSearchQuery,
                                placeholder = { Text("Buscar na pasta...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = outlinedColors()
                            )
                        } else {
                            Column {
                                Text(
                                    text = uiState.serverConfig?.displayName ?: "Explorador",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${uiState.files.size} itens",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SonarCyanPrimary
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (uiState.isSearchActive) {
                                    viewModel.toggleSearchActive()
                                } else {
                                    val navigatedUp = viewModel.navigateUp()
                                    if (!navigatedUp) onNavigateBack()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = viewModel::toggleSearchActive) {
                            Icon(
                                imageVector = if (uiState.isSearchActive) Icons.Filled.Close else Icons.Filled.Search,
                                contentDescription = "Buscar",
                                tint = if (uiState.isSearchActive) SonarCyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Botão para iniciar seleção múltipla
                        IconButton(onClick = viewModel::toggleSelectionMode) {
                            Icon(
                                imageVector = Icons.Filled.Checklist,
                                contentDescription = "Selecionar múltiplos itens",
                                tint = SonarCyanPrimary
                            )
                        }

                        IconButton(onClick = viewModel::toggleGridView) {
                            Icon(
                                imageVector = if (uiState.isGridView) Icons.Filled.ViewList else Icons.Filled.GridView,
                                contentDescription = "Alternar Visualização",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "Mais opções",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Selecionar vários itens") },
                                    leadingIcon = { Icon(Icons.Filled.Checklist, contentDescription = null, tint = SonarCyanPrimary) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.toggleSelectionMode()
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Atualizar") },
                                    leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.refresh()
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text(if (uiState.showHidden) "Ocultar arquivos ocultos" else "Mostrar arquivos ocultos") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (uiState.showHidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        viewModel.toggleShowHidden()
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Ordenar por Nome (A-Z)") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.setSortMode(SortMode.NAME_ASC)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Ordenar por Data") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.setSortMode(SortMode.DATE_DESC)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Ordenar por Tamanho") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.setSortMode(SortMode.SIZE_DESC)
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Barra inferior flutuante com navigationBarsPadding() para respeitar os botões nativos do sistema
            AnimatedVisibility(visible = uiState.isSelectionMode) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 8.dp,
                    border = BorderStroke(1.dp, SonarCyanPrimary.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (uiState.selectedItems.isEmpty()) {
                                    "Toque nos itens para selecionar"
                                } else {
                                    "${uiState.selectedItems.size} item(ns) • ${formatFileSize(uiState.selectedFiles.sumOf { it.sizeBytes })}"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(onClick = viewModel::selectAllFiles) {
                                Text("Selecionar Todos", color = SonarCyanPrimary, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { folderPickerLauncher.launch(null) },
                                enabled = uiState.selectedFiles.isNotEmpty(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (uiState.selectedFiles.isNotEmpty()) SonarCyanPrimary else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FolderOpen,
                                    contentDescription = null,
                                    tint = if (uiState.selectedFiles.isNotEmpty()) SonarCyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Pasta",
                                    color = if (uiState.selectedFiles.isNotEmpty()) SonarCyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            }

                            Button(
                                onClick = viewModel::downloadSelectedFilesToPhone,
                                enabled = uiState.selectedFiles.isNotEmpty(),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(46.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SonarCyanPrimary,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Baixar (${uiState.selectedFiles.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            }

                            OutlinedButton(
                                onClick = { viewModel.openConfirmBatchDeleteDialog(true) },
                                enabled = uiState.selectedItems.isNotEmpty(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (uiState.selectedItems.isNotEmpty()) StatusError else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = if (uiState.selectedItems.isNotEmpty()) StatusError else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Apagar",
                                    color = if (uiState.selectedItems.isNotEmpty()) StatusError else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FloatingActionButton(
                        onClick = { viewModel.openCreateFolderDialog(true) },
                        modifier = Modifier.size(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = SonarCyanPrimary,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CreateNewFolder,
                            contentDescription = "Nova Pasta",
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    FloatingActionButton(
                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        containerColor = SonarCyanPrimary,
                        contentColor = Color.Black,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.UploadFile,
                            contentDescription = "Enviar Arquivo",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Barra de Navegação de Migalhas (Breadcrumb Bar)
            BreadcrumbBar(
                currentPath = uiState.currentPath,
                onNavigateToSegment = viewModel::navigateToBreadcrumb,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Card com Porcentagem em Tempo Real de Upload e Download
            AnimatedVisibility(visible = uiState.activeTransfers.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, SonarCyanPrimary.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        uiState.activeTransfers.forEachIndexed { index, task ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            }
                            ActiveTransferRow(
                                task = task,
                                onCancel = { viewModel.cancelTransfer(task) }
                            )
                        }
                    }
                }
            }

            // Mensagem de Erro
            if (!uiState.errorMessage.isNullOrBlank()) {
                Surface(
                    color = StatusError.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = uiState.errorMessage!!,
                        color = StatusError,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Conteúdo Principal
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoading -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = SonarCyanPrimary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (uiState.isAuthenticating) "Autenticando sessão segura..." else "Carregando arquivos...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                                        imageVector = Icons.Filled.FolderOpen,
                                        contentDescription = null,
                                        tint = SonarCyanPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Esta pasta está vazia",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    uiState.isGridView -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 105.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(items = uiState.files, key = { it.fullPath }) { file ->
                                val isSelected = file.fullPath in uiState.selectedFilePaths
                                FileItemGrid(
                                    file = file,
                                    isSelectionMode = uiState.isSelectionMode,
                                    isSelected = isSelected,
                                    onClick = {
                                        if (uiState.isSelectionMode) {
                                            viewModel.toggleFileSelection(file)
                                        } else {
                                            if (file.isDirectory) {
                                                viewModel.navigateTo(file.fullPath)
                                            } else {
                                                viewModel.openFile(file)
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        viewModel.toggleFileSelection(file)
                                    },
                                    onOptionsClick = {
                                        viewModel.selectFileForAction(file)
                                    }
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(items = uiState.files, key = { it.fullPath }) { file ->
                                val isSelected = file.fullPath in uiState.selectedFilePaths
                                FileItemRow(
                                    file = file,
                                    isSelectionMode = uiState.isSelectionMode,
                                    isSelected = isSelected,
                                    onClick = {
                                        if (uiState.isSelectionMode) {
                                            viewModel.toggleFileSelection(file)
                                        } else {
                                            if (file.isDirectory) {
                                                viewModel.navigateTo(file.fullPath)
                                            } else {
                                                viewModel.openFile(file)
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        viewModel.toggleFileSelection(file)
                                    },
                                    onOptionsClick = {
                                        viewModel.selectFileForAction(file)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal BottomSheet para ações em arquivo selecionado
    if (uiState.selectedFileForAction != null) {
        val file = uiState.selectedFileForAction!!
        ModalBottomSheet(
            onDismissRequest = { viewModel.selectFileForAction(null) },
            sheetState = rememberModalBottomSheetState()
        ) {
            FileActionBottomSheetContent(
                file = file,
                onOpen = {
                    viewModel.selectFileForAction(null)
                    viewModel.openFile(file)
                },
                onSaveAs = {
                    fileToSave = file
                    viewModel.selectFileForAction(null)
                    createDocumentLauncher.launch(file.name)
                },
                onQuickDownload = {
                    viewModel.downloadFileToPhone(file)
                },
                onSelectMultiple = {
                    viewModel.selectFileForAction(null)
                    viewModel.toggleFileSelection(file)
                },
                onRename = {
                    viewModel.openRenameDialog(true)
                },
                onDelete = {
                    viewModel.openConfirmDeleteDialog(true)
                }
            )
        }
    }

    // Diálogo de Carregando ao Abrir Arquivo
    if (uiState.isOpeningFile) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Abrindo Arquivo", fontWeight = FontWeight.Bold) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = SonarCyanPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Carregando ${uiState.openingFileName ?: "arquivo"}...")
                }
            }
        )
    }

    // Diálogos de Ação
    if (uiState.isCreateFolderDialogOpen) {
        CreateFolderDialog(
            onDismiss = { viewModel.openCreateFolderDialog(false) },
            onConfirm = viewModel::createFolder
        )
    }

    if (uiState.isRenameDialogOpen && uiState.selectedFileForAction != null) {
        RenameFileDialog(
            initialName = uiState.selectedFileForAction!!.name,
            onDismiss = { viewModel.openRenameDialog(false) },
            onConfirm = viewModel::renameSelectedFile
        )
    }

    if (uiState.isConfirmDeleteDialogOpen && uiState.selectedFileForAction != null) {
        ConfirmDeleteDialog(
            fileName = uiState.selectedFileForAction!!.name,
            isDirectory = uiState.selectedFileForAction!!.isDirectory,
            onDismiss = { viewModel.openConfirmDeleteDialog(false) },
            onConfirm = viewModel::deleteSelectedFile
        )
    }

    if (uiState.isConfirmBatchDeleteDialogOpen && uiState.selectedItems.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { viewModel.openConfirmBatchDeleteDialog(false) },
            title = { Text("Apagar Selecionados", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Tem certeza de que deseja apagar permanentemente ${uiState.selectedItems.size} item(ns) selecionado(s) do servidor?"
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::deleteSelectedItems,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusError,
                        contentColor = Color.White
                    )
                ) {
                    Text("Apagar Todos", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.openConfirmBatchDeleteDialog(false) }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun FileActionBottomSheetContent(
    file: RemoteFile,
    onOpen: () -> Unit,
    onSaveAs: () -> Unit,
    onQuickDownload: () -> Unit,
    onSelectMultiple: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .padding(bottom = 16.dp)
    ) {
        // Cabeçalho com ícone e nome do arquivo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            val (icon, tint) = getFileIconAndTint(file)
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = tint.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = tint)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (file.isDirectory) "Pasta" else "${formatFileSize(file.sizeBytes)} • ${formatDate(file.lastModifiedEpochMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column {
                if (!file.isDirectory) {
                    TextButton(
                        onClick = onOpen,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = null, tint = SonarCyanPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Abrir Arquivo", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    TextButton(
                        onClick = onSaveAs,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = SonarCyanPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Salvar no Telefone (Escolher pasta)...", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    TextButton(
                        onClick = onQuickDownload,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Download, contentDescription = null, tint = SonarCyanPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Download Rápido (Pasta Downloads)", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                TextButton(
                    onClick = onSelectMultiple,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Checklist, contentDescription = null, tint = SonarCyanPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Selecionar vários itens", color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                TextButton(
                    onClick = onRename,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = null, tint = SonarCyanPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Renomear", color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = StatusError)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Excluir", color = StatusError)
                    }
                }
            }
        }
    }
}

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (folderName: String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova Pasta", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("Nome da pasta") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedColors()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (folderName.isNotBlank()) onConfirm(folderName.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = SonarCyanPrimary, contentColor = Color.Black),
                enabled = folderName.isNotBlank()
            ) {
                Text("Criar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun RenameFileDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit
) {
    var newName by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renomear", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Novo nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedColors()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.isNotBlank() && newName != initialName) onConfirm(newName.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = SonarCyanPrimary, contentColor = Color.Black),
                enabled = newName.isNotBlank() && newName != initialName
            ) {
                Text("Renomear", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ConfirmDeleteDialog(
    fileName: String,
    isDirectory: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar Exclusão", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "Tem certeza de que deseja excluir permanentemente ${if (isDirectory) "a pasta" else "o arquivo"} '$fileName' do servidor remoto?"
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = StatusError, contentColor = Color.White)
            ) {
                Text("Excluir", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ActiveTransferRow(
    task: TransferTask,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val percent = (task.progressFraction * 100).toInt()
    val isUpload = task.type == TransferType.UPLOAD
    val icon = if (isUpload) Icons.Filled.Upload else Icons.Filled.Download
    val actionLabel = if (isUpload) "Enviando" else "Baixando"

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SonarCyanPrimary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = SonarCyanPrimary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { task.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = SonarCyanPrimary,
                trackColor = MaterialTheme.colorScheme.background
            )

            Spacer(modifier = Modifier.height(3.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$actionLabel • ${formatFileSize(task.transferredBytes)} de ${formatFileSize(task.totalBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (task.speedBytesPerSec > 0) {
                    Text(
                        text = "${formatFileSize(task.speedBytesPerSec)}/s",
                        style = MaterialTheme.typography.labelSmall,
                        color = SonarCyanPrimary
                    )
                }
            }
        }

        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .padding(start = 6.dp)
                .size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Cancelar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
