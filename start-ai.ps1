param(
    [string]$Model = 'qwen2.5:1.5b',
    [string]$EmbeddingModel = 'nomic-embed-text'
)

$ErrorActionPreference = 'Stop'
$compose = Join-Path $PSScriptRoot 'scripts\ollama.compose.yaml'
$endpoint = 'http://127.0.0.1:11435'
$docker = (Get-Command docker -ErrorAction Stop).Source

function Invoke-AiCompose {
    & $docker compose -p vetclinic-ai -f $compose @args
    if ($LASTEXITCODE -ne 0) { throw "Docker Compose failed ($LASTEXITCODE)." }
}

Invoke-AiCompose up -d ollama
$installed = $null
for ($attempt = 0; $attempt -lt 30; $attempt++) {
    try {
        $installed = Invoke-RestMethod "$endpoint/api/tags" -TimeoutSec 2
        break
    } catch {
        Start-Sleep -Seconds 1
    }
}
if ($null -eq $installed) { throw "Ollama did not respond at $endpoint." }

foreach ($tag in @($Model, $EmbeddingModel) | Select-Object -Unique) {
    $name = if ($tag.Contains(':')) { $tag } else { "${tag}:latest" }
    if ($installed.models.name -notcontains $name) {
        Invoke-AiCompose exec -T ollama ollama pull $tag
    }
}

# Verify both models with a synthetic input before sending any clinic records.
$headers = 'application/json; charset=utf-8'
$embedding = Invoke-RestMethod "$endpoint/api/embed" -Method Post -ContentType $headers -TimeoutSec 90 -Body (
    @{model=$EmbeddingModel; input='VetClinic readiness check'} | ConvertTo-Json -Compress
)
if (!$embedding.embeddings -or !$embedding.embeddings[0].Count) { throw 'Embedding model returned no vector.' }

$chat = Invoke-RestMethod "$endpoint/api/chat" -Method Post -ContentType $headers -TimeoutSec 90 -Body (
    @{model=$Model; stream=$false; options=@{num_predict=24}; messages=@(@{role='user'; content='Reply with OK.'})} | ConvertTo-Json -Depth 4 -Compress
)
if ([string]::IsNullOrWhiteSpace($chat.message.content)) { throw 'Chat model returned no text.' }

$settings = @{
    APP_AI_ENABLED = 'true'
    APP_AI_URL = $endpoint
    APP_AI_MODEL = $Model
    APP_AI_EMBEDDING_MODEL = $EmbeddingModel
}
$previous = @{}
try {
    foreach ($key in $settings.Keys) {
        $previous[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
        [Environment]::SetEnvironmentVariable($key, $settings[$key], 'Process')
    }
    & (Join-Path $PSScriptRoot 'stop-postgres-app.ps1')
    & (Join-Path $PSScriptRoot 'start-postgres.ps1')
    Write-Output "VetClinic AI is ready at $endpoint."
} finally {
    foreach ($key in $previous.Keys) {
        [Environment]::SetEnvironmentVariable($key, $previous[$key], 'Process')
    }
}