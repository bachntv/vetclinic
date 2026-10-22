$ErrorActionPreference = 'Stop'
$backendPath = Split-Path -Parent $PSScriptRoot
$projectPath = Split-Path -Parent $backendPath
$backupDirectory = Join-Path $projectPath 'backups'
$backupName = 'vetclinic-before-upgrade-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.dump'
$containerBackup = '/tmp/' + $backupName
$localBackup = Join-Path $backupDirectory $backupName
$migrations = @('002_staff_workspace.sql', '003_report_alignment.sql')

function Assert-DockerSuccess([string]$step) {
    if ($LASTEXITCODE -ne 0) { throw "Docker failed: $step. Migration has not completed. Keep the backup and review the error." }
}

Push-Location $backendPath
try {
    Write-Host 'Stop the backend before running this script. Docker Desktop must be running.'
    $containerId = & docker compose ps -q postgres
    Assert-DockerSuccess 'find PostgreSQL container'
    if (-not $containerId) { throw 'PostgreSQL container is not running. Run docker compose up -d first.' }
    $containerId = ($containerId | Select-Object -First 1).Trim()

    $countQuery = "SELECT 'users',count(*) FROM users UNION ALL SELECT 'owners',count(*) FROM owners UNION ALL SELECT 'pets',count(*) FROM pets UNION ALL SELECT 'vets',count(*) FROM vets UNION ALL SELECT 'visits',count(*) FROM visits UNION ALL SELECT 'vaccinations',count(*) FROM vaccinations UNION ALL SELECT 'pet_weight_records',count(*) FROM pet_weight_records UNION ALL SELECT 'medicines',count(*) FROM medicines UNION ALL SELECT 'prescriptions',count(*) FROM prescriptions UNION ALL SELECT 'prescription_items',count(*) FROM prescription_items UNION ALL SELECT 'invoices',count(*) FROM invoices UNION ALL SELECT 'invoice_items',count(*) FROM invoice_items ORDER BY 1;"
    $before = & docker compose exec -T postgres psql -U postgres -d vetclinic -At -c $countQuery
    Assert-DockerSuccess 'read counts before upgrade'

    Write-Host 'Creating PostgreSQL custom-format backup...'
    & docker compose exec -T postgres pg_dump -U postgres -d vetclinic --format=custom --file=$containerBackup
    Assert-DockerSuccess 'pg_dump'
    & docker compose exec -T postgres pg_restore --list $containerBackup | Out-Null
    Assert-DockerSuccess 'validate backup archive'
    New-Item -ItemType Directory -Path $backupDirectory -Force | Out-Null
    & docker cp "${containerId}:${containerBackup}" $localBackup
    Assert-DockerSuccess 'copy backup to host'
    if ((Get-Item -LiteralPath $localBackup).Length -le 0) { throw 'Empty backup file. Migration will not run.' }
    $checksum = (Get-FileHash -LiteralPath $localBackup -Algorithm SHA256).Hash
    Set-Content -LiteralPath ($localBackup + '.sha256') -Value $checksum -Encoding ASCII
    Write-Host "Backup verified: $localBackup"

    foreach ($name in $migrations) {
        $migration = Join-Path $backendPath ('docs\migrations\' + $name)
        if (-not (Test-Path -LiteralPath $migration)) { throw "Missing migration: $migration" }
        Write-Host "Applying transactional migration $name..."
        Get-Content -LiteralPath $migration -Raw -Encoding UTF8 | & docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U postgres -d vetclinic
        Assert-DockerSuccess "apply $name"
    }

    $after = & docker compose exec -T postgres psql -U postgres -d vetclinic -At -c $countQuery
    Assert-DockerSuccess 'read counts after upgrade'
    if (($before -join "`n") -ne ($after -join "`n")) { throw 'Row counts changed. Keep backup and inspect concurrent writes before continuing.' }
    $tables = & docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U postgres -d vetclinic -At -c "SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename IN ('support_requests','knowledge_documents','vaccine_types','revoked_tokens') ORDER BY tablename;"
    Assert-DockerSuccess 'verify added tables'
    if (($tables -join ',') -ne 'knowledge_documents,revoked_tokens,support_requests,vaccine_types') { throw 'Missing expected tables after migration.' }
    $columns = & docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U postgres -d vetclinic -At -c "SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND (table_name,column_name) IN (('visits','symptoms'),('medicines','description'),('vaccinations','notes'),('vaccinations','vaccine_id'),('users','vet_id'));"
    Assert-DockerSuccess 'verify added columns'
    if (($columns -join '').Trim() -ne '5') { throw 'Missing expected columns after migration.' }
    Write-Host 'SUCCESS: schema upgraded; counts of all 12 original tables unchanged.'
    Write-Host 'Restart backend, then sign out/in on the frontend. Existing email/password stays unchanged.'
}
finally {
    Pop-Location
}
