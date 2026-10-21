package com.sonarfiles.app.presentation.screens.explorer

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreateFolderDialog(true) },
                containerColor = SonarCyanPrimary,
                contentColor = Color.Black
            ) {
                Icon(Icons.Filled.CreateNewFolder, contentDescription = "Nova Pasta")
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
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(items = uiState.files, key = { it.fullPath }) { file ->
                                FileItemGrid(
                                    file = file,
                                    onClick = {
                                        if (file.isDirectory) {
                                            viewModel.navigateTo(file.fullPath)
                                        }
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
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(items = uiState.files, key = { it.fullPath }) { file ->
                                FileItemRow(
                                    file = file,
                                    onClick = {
                                        if (file.isDirectory) {
                                            viewModel.navigateTo(file.fullPath)
                                        }
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
                onRename = {
                    viewModel.openRenameDialog(true)
                },
                onDelete = {
                    viewModel.openConfirmDeleteDialog(true)
                }
            )
        }
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
}

@Composable
fun FileActionBottomSheetContent(
    file: RemoteFile,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .padding(bottom = 24.dp)
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
