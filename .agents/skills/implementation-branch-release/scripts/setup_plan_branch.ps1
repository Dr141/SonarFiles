param(
    [Parameter(Mandatory=$true)]
    [string]$BranchName,

    [string]$BaseBranch = "main"
)

$ErrorActionPreference = "Stop"

Write-Host "🔍 Verificando status do Git..." -ForegroundColor Cyan

# Garante que está no diretório correto do projeto
if (Test-Path "SonarFiles") {
    Set-Location "SonarFiles"
}

# Verifica se está em um repositório git
$isGit = git rev-parse --is-inside-work-tree 2>$null
if ($isGit -ne "true") {
    Write-Error "O diretório atual não é um repositório Git."
    exit 1
}

# Verifica branch base
Write-Host "📡 Atualizando branch base '$BaseBranch'..." -ForegroundColor Cyan
git fetch origin $BaseBranch 2>$null

# Cria e alterna para a nova branch
Write-Host "🌿 Criando e alternando para a branch '$BranchName'..." -ForegroundColor Green
git checkout -b $BranchName

Write-Host "✅ Branch '$BranchName' criada e ativa com sucesso para o novo plano de implementação!" -ForegroundColor Green
