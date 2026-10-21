param(
    [Parameter(Mandatory=$true)]
    [string]$Version,

    [string]$CommitMessage = ""
)

$ErrorActionPreference = "Stop"

# Garante que está no diretório correto do projeto
if (Test-Path "SonarFiles") {
    Set-Location "SonarFiles"
}

# Normaliza versão para ter o prefixo 'v' na tag
$cleanVersion = $Version.TrimStart('v')
$tagVersion = "v$cleanVersion"

if ([string]::IsNullOrWhiteSpace($CommitMessage)) {
    $CommitMessage = "feat: implement plan and release $tagVersion"
}

Write-Host "📦 Finalizando implementação para versão $tagVersion..." -ForegroundColor Cyan

# 1. Verifica se há arquivos modificados
$status = git status --porcelain
if ($status) {
    Write-Host "📝 Adicionando alterações ao stage..." -ForegroundColor Cyan
    git add .
    Write-Host "💾 Criando commit: '$CommitMessage'..." -ForegroundColor Cyan
    git commit -m "$CommitMessage"
} else {
    Write-Host "ℹ️ Nenhuma alteração pendente para commit." -ForegroundColor Yellow
}

# 2. Obtém a branch atual
$currentBranch = (git branch --show-current).Trim()
Write-Host "🌿 Branch atual: $currentBranch" -ForegroundColor Cyan

# 3. Cria a tag anotada
Write-Host "🏷️ Criando tag anotada '$tagVersion'..." -ForegroundColor Green
git tag -a "$tagVersion" -m "Release $tagVersion: implementação concluída" -f

# 4. Push da branch e da tag
Write-Host "🚀 Enviando branch '$currentBranch' para o GitHub (origin)..." -ForegroundColor Cyan
git push -u origin $currentBranch

Write-Host "🚀 Enviando tag '$tagVersion' para o GitHub (origin)..." -ForegroundColor Green
git push origin "$tagVersion"

Write-Host "🎉 Sucesso! A branch '$currentBranch' e a tag '$tagVersion' foram enviadas." -ForegroundColor Green
Write-Host "⚡ O workflow do GitHub Actions (release.yml) foi disparado automaticamente!" -ForegroundColor Magenta
