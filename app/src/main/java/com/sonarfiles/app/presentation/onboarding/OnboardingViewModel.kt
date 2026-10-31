package com.sonarfiles.app.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.sonarfiles.app.data.local.preferences.OnboardingPreferences
import com.sonarfiles.app.presentation.navigation.ScreenRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingPreferences: OnboardingPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OnboardingTourUiState(
            isTourActive = false,
            currentStepIndex = 0,
            totalSteps = TourStepsProvider.steps.size,
            currentStep = TourStepsProvider.steps.firstOrNull()
        )
    )
    val uiState: StateFlow<OnboardingTourUiState> = _uiState.asStateFlow()

    private var initialCheckDone = false

    init {
        checkFirstTimeAccess()
    }

    private fun checkFirstTimeAccess() {
        if (initialCheckDone) return
        initialCheckDone = true

        viewModelScope.launch {
            val hasCompleted = onboardingPreferences.hasCompletedOnboarding.first()
            if (!hasCompleted) {
                _uiState.update {
                    it.copy(
                        isTourActive = true,
                        currentStepIndex = 0,
                        currentStep = TourStepsProvider.steps[0]
                    )
                }
            }
        }
    }

    fun startTour(navController: NavHostController?) {
        navController?.popBackStack(ScreenRoutes.Home, inclusive = false)
        _uiState.update {
            it.copy(
                isTourActive = true,
                currentStepIndex = 0,
                currentStep = TourStepsProvider.steps[0]
            )
        }
    }

    fun nextStep(navController: NavHostController) {
        val currentIdx = _uiState.value.currentStepIndex
        val nextIdx = currentIdx + 1

        if (nextIdx >= TourStepsProvider.steps.size) {
            finishTour(navController)
            return
        }

        _uiState.update {
            it.copy(
                currentStepIndex = nextIdx,
                currentStep = TourStepsProvider.steps[nextIdx]
            )
        }

        navigateToStepScreen(nextIdx, navController)
    }

    fun previousStep(navController: NavHostController) {
        val currentIdx = _uiState.value.currentStepIndex
        val prevIdx = (currentIdx - 1).coerceAtLeast(0)

        _uiState.update {
            it.copy(
                currentStepIndex = prevIdx,
                currentStep = TourStepsProvider.steps[prevIdx]
            )
        }

        navigateToStepScreen(prevIdx, navController)
    }

    private fun navigateToStepScreen(stepIndex: Int, navController: NavHostController) {
        when (stepIndex) {
            0 -> {
                // Passo 1: Visão Geral na Home
                navController.popBackStack(ScreenRoutes.Home, inclusive = false)
            }
            1 -> {
                // Passo 2: Botões Flutuantes na Home
                navController.popBackStack(ScreenRoutes.Home, inclusive = false)
            }
            2 -> {
                // Passo 3: Sonar Scan
                navController.popBackStack(ScreenRoutes.Home, inclusive = false)
                navController.navigate(ScreenRoutes.SonarScan)
            }
            3 -> {
                // Passo 4: Formulário de Servidor
                navController.popBackStack(ScreenRoutes.Home, inclusive = false)
                navController.navigate(ScreenRoutes.ServerForm())
            }
            4 -> {
                // Passo 5: Explorador de Arquivos (Explicação com visual ilustrativo)
                navController.popBackStack(ScreenRoutes.Home, inclusive = false)
            }
            5 -> {
                // Passo 6: Central de Transferências
                navController.popBackStack(ScreenRoutes.Home, inclusive = false)
                navController.navigate(ScreenRoutes.Transfers)
            }
        }
    }

    fun skipTour(navController: NavHostController) {
        viewModelScope.launch {
            onboardingPreferences.setOnboardingCompleted(true)
            _uiState.update { it.copy(isTourActive = false) }
            navController.popBackStack(ScreenRoutes.Home, inclusive = false)
        }
    }

    private fun finishTour(navController: NavHostController) {
        viewModelScope.launch {
            onboardingPreferences.setOnboardingCompleted(true)
            _uiState.update { it.copy(isTourActive = false) }
            navController.popBackStack(ScreenRoutes.Home, inclusive = false)
        }
    }
}
