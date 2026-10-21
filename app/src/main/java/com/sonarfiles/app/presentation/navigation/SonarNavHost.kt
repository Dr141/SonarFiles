package com.sonarfiles.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.sonarfiles.app.domain.model.ProtocolType
import com.sonarfiles.app.presentation.screens.explorer.ExplorerScreen
import com.sonarfiles.app.presentation.screens.explorer.ExplorerViewModel
import com.sonarfiles.app.presentation.screens.home.HomeScreen
import com.sonarfiles.app.presentation.screens.home.HomeViewModel
import com.sonarfiles.app.presentation.screens.server_form.ServerFormScreen
import com.sonarfiles.app.presentation.screens.server_form.ServerFormViewModel
import com.sonarfiles.app.presentation.screens.sonar.SonarScreen
import com.sonarfiles.app.presentation.screens.sonar.SonarViewModel
import com.sonarfiles.app.presentation.screens.transfers.TransfersScreen
import com.sonarfiles.app.presentation.screens.transfers.TransfersViewModel

@Composable
fun SonarNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = ScreenRoutes.Home,
        modifier = modifier
    ) {
        composable<ScreenRoutes.Home> {
            val homeViewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToSonar = {
                    navController.navigate(ScreenRoutes.SonarScan)
                },
                onNavigateToManualAdd = {
                    navController.navigate(ScreenRoutes.ServerForm())
                },
                onNavigateToExplorer = { serverId ->
                    navController.navigate(ScreenRoutes.Explorer(serverId = serverId))
                },
                onNavigateToTransfers = {
                    navController.navigate(ScreenRoutes.Transfers)
                },
                onSendWakeOnLan = { mac ->
                    homeViewModel.sendWakeOnLan(mac)
                }
            )
        }

        composable<ScreenRoutes.SonarScan> {
            val sonarViewModel: SonarViewModel = hiltViewModel()
            SonarScreen(
                viewModel = sonarViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSelectHost = { host, protocol ->
                    navController.navigate(
                        ScreenRoutes.ServerForm(
                            initialHost = host.ipAddress,
                            initialProtocol = protocol,
                            initialHostname = host.hostname
                        )
                    )
                }
            )
        }

        composable<ScreenRoutes.ServerForm> { backStackEntry ->
            val route = backStackEntry.toRoute<ScreenRoutes.ServerForm>()
            val serverFormViewModel: ServerFormViewModel = hiltViewModel()

            LaunchedEffect(route) {
                if (route.initialHost != null) {
                    serverFormViewModel.prefillFromDiscoveredHost(
                        ip = route.initialHost,
                        protocol = route.initialProtocol ?: ProtocolType.SMB,
                        suggestedHostname = route.initialHostname
                    )
                }
            }

            ServerFormScreen(
                viewModel = serverFormViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSavedSuccess = {
                    navController.popBackStack(ScreenRoutes.Home, inclusive = false)
                }
            )
        }

        composable<ScreenRoutes.Explorer> { backStackEntry ->
            val route = backStackEntry.toRoute<ScreenRoutes.Explorer>()
            val explorerViewModel: ExplorerViewModel = hiltViewModel()

            ExplorerScreen(
                viewModel = explorerViewModel,
                serverId = route.serverId,
                initialPath = route.initialPath,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<ScreenRoutes.Transfers> {
            val transfersViewModel: TransfersViewModel = hiltViewModel()
            TransfersScreen(
                viewModel = transfersViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
