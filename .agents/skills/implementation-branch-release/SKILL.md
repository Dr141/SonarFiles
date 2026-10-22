---
name: implementation-branch-release
description: >-
  Use esta skill sempre que o usuário for implementar um novo plano de implementação ou feature no projeto SonarFiles.
  Ela padroniza o ciclo de vida Git: criação de uma nova branch dedicada antes do início da implementação,
  geração de commits semânticos, bump de versão (versionName/versionCode), criação da tag semântica (ex: v1.1.0)
  e push da branch e da tag para o GitHub, acionando a pipeline de release automática (.github/workflows/release.yml).
---

# Fluxo de Branch e Release Automática para Planos de Implementação

Esta skill estabelece o procedimento padrão para implementar qualquer novo plano de desenvolvimento, funcionalidade ou correção no projeto **SonarFiles**, garantindo isolamento em branch dedicada e acionamento automático da esteira de CI/CD via Git Tags.

---

## 🧭 Visão Geral do Ciclo de Vida

```text
[Início do Plano] ──> Cria nova branch (feature/* ou release/*)
         │
         ▼
[Desenvolvimento] ──> Edição, criação de código e testes
         │
         ▼
[Finalização]    ──> Bump de versão (versionCode/versionName)
         │
         ▼
[Commit & Tag]   ──> git commit + git tag -a vX.X.X
         │
         ▼
[Push Remoto]    ──> git push origin <branch> && git push origin <tag>
         │
         ▼
[GitHub Actions] ──> release.yml compila APK/AAB, publica GitHub Release,
                     cria Pull Request e mescla com a main automaticamente!
```

---

## 🛠️ Procedimento Passo a Passo

### Passo 1: Início do Plano — Criar Branch Dedicada

Sempre antes de gerar ou modificar código de um novo plano de desenvolvimento:

1. Defina um nome descritivo para a branch seguindo a convenção:
   - `feature/<nome-da-funcionalidade>` (ex: `feature/audio-stream-player`, `feature/samba-rap-share-discovery`)
   - `release/v<versao>` (ex: `release/v1.1.0`)
   - `fix/<nome-do-bug>` (ex: `fix/smb-timeout-handling`)

2. Crie e alterne para a branch a partir da branch base atualizada (`main`):
   ```powershell
   git checkout -b <nome-da-branch>
   ```
   *(Ou execute o script auxiliar: `powershell .agents/skills/implementation-branch-release/scripts/setup_plan_branch.ps1 -BranchName "<nome-da-branch>" -BaseBranch "main"`)*

3. Confirme que a branch está ativa com `git branch --show-current`.

---

### Passo 2: Implementação do Código

Execute a implementação técnica descrita no plano, respeitando a arquitetura existente do **SonarFiles**:
- Camada `domain` desacoplada de bibliotecas de rede.
- Camada `data` usando `RemoteFileSystem` (SMBJ / SSHJ).
- Camada `presentation` com Jetpack Compose Material 3 (UDF e AMOLED).

---

### Passo 3: Atualização de Versão (Bump Version)

Se o plano entrega uma nova versão pública ou release:

1. Atualize o `versionCode` (incremento numérico inteiro) e o `versionName` no arquivo [`app/build.gradle.kts`](file:///c:/Users/usuarioDM/Documents/Android/SonarFiles/app/build.gradle.kts):
   ```kotlin
   defaultConfig {
       versionCode = 2       // incrementa +1
       versionName = "1.1.0" // nova versão semântica
   }
   ```
2. Se aplicável, atualize a versão correspondente no catálogo [`gradle/libs.versions.toml`](file:///c:/Users/usuarioDM/Documents/Android/SonarFiles/gradle/libs.versions.toml).

---

### Passo 4: Commit das Alterações

Crie um commit estruturado seguindo o padrão **Conventional Commits**:
```powershell
git add .
git commit -m "feat: <descrição sucinta das alterações do plano>"
```

Exemplos de mensagens:
- `feat: implement audio streaming player with media3`
- `feat: add network radar sweep animations`
- `fix: resolve credential decryption padding error`

---

### Passo 5: Criar a Tag e Enviar para o GitHub (Push)

O envio da tag Git com o prefixo `v` (ex: `v1.1.0`) aciona automaticamente o workflow de release [`.github/workflows/release.yml`](file:///c:/Users/usuarioDM/Documents/Android/SonarFiles/.github/workflows/release.yml) no GitHub Actions:

1. Crie a tag anotada:
   ```powershell
   git tag -a v1.1.0 -m "Release v1.1.0: <resumo das funcionalidades>"
   ```

2. Envie a branch atual e a nova tag para o repositório remoto:
   ```powershell
   # Envia a branch de trabalho
   git push -u origin <nome-da-branch>

   # Envia a tag (dispara o GitHub Actions)
   git push origin v1.1.0
   ```
   *(Ou execute o script: `powershell .agents/skills/implementation-branch-release/scripts/finish_plan_release.ps1 -Version "1.1.0" -CommitMessage "feat: implement plan and bump to v1.1.0"`)*

---

## 🔍 Verificação Pós-Execução

Após o push da tag:
1. Acesse o GitHub: `https://github.com/<usuario>/<repositorio>/actions`.
2. Verifique a execução do job **"Release Build & Publish"**.
3. Confirme a publicação do APK (`SonarFiles-v1.1.0.apk`) e AAB em **Releases**.
4. Confirme que o Pull Request foi gerado e mesclado automaticamente na branch `main`.
