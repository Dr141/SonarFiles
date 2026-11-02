package com.sonarfiles.app.presentation.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

enum class TourHighlightZone {
    SERVERS_OVERVIEW,
    FLOATING_ACTIONS,
    SONAR_RADAR,
    SERVER_FORM,
    EXPLORER_ACTIONS,
    TRANSFERS_CENTER
}

data class OnboardingStep(
    val stepIndex: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val description: String,
    val tips: List<String>,
    val highlightZone: TourHighlightZone,
    val nextButtonLabel: String = "Próximo"
)

data class OnboardingTourUiState(
    val isTourActive: Boolean = false,
    val currentStepIndex: Int = 0,
    val totalSteps: Int = 6,
    val currentStep: OnboardingStep? = null
)

object TourStepsProvider {
    val steps = listOf(
        OnboardingStep(
            stepIndex = 0,
            title = "Bem-vindo ao SonarFiles!",
            subtitle = "Gerenciamento de Rede Rápido e Seguro",
            icon = Icons.Filled.Dns,
            description = "O SonarFiles conecta seu celular aos seus computadores Windows (SMB) e servidores Linux (SFTP) diretamente pela rede Wi-Fi local, sem depender de internet ou nuvem.",
            tips = listOf(
                "🟢 Bolinha Verde: Servidor online e pronto para conectar.",
                "🔴 Bolinha Vermelha: Servidor inacessível no momento.",
                "Alternância de Grade ou Lista no topo para melhor visualização."
            ),
            highlightZone = TourHighlightZone.SERVERS_OVERVIEW
        ),
        OnboardingStep(
            stepIndex = 1,
            title = "Como Conectar aos Servidores",
            subtitle = "Dois Métodos Práticos no Canto Inferior",
            icon = Icons.Filled.Radar,
            description = "Os botões flutuantes no canto inferior direito dão acesso instantâneo às conexões:",
            tips = listOf(
                "📡 Botão Sonar Scan (Radar): Varre a rede Wi-Fi e localiza computadores automaticamente.",
                "➕ Botão Novo Servidor (+): Permite cadastrar manualmente IP, porta, usuário e senha."
            ),
            highlightZone = TourHighlightZone.FLOATING_ACTIONS
        ),
        OnboardingStep(
            stepIndex = 2,
            title = "Sonar Network (Varredura Automática)",
            subtitle = "Localização de Dispositivos e Portas",
            icon = Icons.Filled.Wifi,
            description = "O Sonar varre toda a faixa da sua rede local (ex: 192.168.1.X) identificando se há portas SMB (445) ou SFTP (22) abertas.",
            tips = listOf(
                "Detecta o sistema operacional (Windows, Linux, Mac).",
                "Ao tocar em um computador descoberto, os dados são preenchidos automaticamente para você."
            ),
            highlightZone = TourHighlightZone.SONAR_RADAR
        ),
        OnboardingStep(
            stepIndex = 3,
            title = "Configuração & Diagnóstico em 4 Etapas",
            subtitle = "Teste Antes de Conectar",
            icon = Icons.Filled.Computer,
            description = "No cadastro do servidor, você conta com um assistente inteligente completo:",
            tips = listOf(
                "🔍 Teste de Conexão: Valida IP, Porta, Senha e Pasta Raiz antes de salvar.",
                "❓ Guia Passo a Passo: Instruções completas para compartilhar pastas no Windows e Linux.",
                "🔒 Biometria: Proteja o acesso às suas pastas com a digital."
            ),
            highlightZone = TourHighlightZone.SERVER_FORM
        ),
        OnboardingStep(
            stepIndex = 4,
            title = "Explorador de Arquivos & Ações",
            subtitle = "Produtividade Completa no Dispositivo",
            icon = Icons.Filled.FolderShared,
            description = "Navegue pelas pastas compartilhadas do seu computador com velocidade máxima e ferramentas poderosas:",
            tips = listOf(
                "📂 Abrir Direto: Toque em qualquer foto, vídeo ou documento para abrir no celular.",
                "✅ Seleção Múltipla: Selecione vários itens com 'Selecionar Todos' ou 'Desmarcar Todos'.",
                "📥 Escolher Pasta: Salve onde quiser no telefone tocando no botão com ícone (✔).",
                "🗑️ Apagar em Lote: Exclua vários arquivos e pastas de uma só vez do servidor remoto."
            ),
            highlightZone = TourHighlightZone.EXPLORER_ACTIONS
        ),
        OnboardingStep(
            stepIndex = 5,
            title = "Central de Transferências",
            subtitle = "Downloads e Uploads com Porcentagem Real",
            icon = Icons.Filled.Sync,
            description = "Acompanhe todo o progresso em segundo plano diretamente pela barra ou pela tela de Transferências.",
            tips = listOf(
                "📊 Progresso em tempo real com % exata, bytes transferidos e velocidade (MB/s).",
                "🔄 Atualização automática da pasta remota assim que o upload for concluído.",
                "Suporte a cancelamento e fila contínua."
            ),
            highlightZone = TourHighlightZone.TRANSFERS_CENTER,
            nextButtonLabel = "Concluir e Usar App"
        )
    )
}
