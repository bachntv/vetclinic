$ErrorActionPreference='Stop'
$root=$PSScriptRoot
$file=Join-Path $root '.runtime/postgres-processes.json'
if(!(Test-Path -LiteralPath $file)){Write-Output 'Không có thông tin tiến trình VetClinic.';exit}
$state=Get-Content -Raw -LiteralPath $file|ConvertFrom-Json
foreach($processId in @($state.backendPid,$state.frontendPid)){
 $process=Get-CimInstance Win32_Process -Filter "ProcessId=$processId" -ErrorAction SilentlyContinue
 if(!$process){continue}
 if(!$process.CommandLine -or !$process.CommandLine.Contains($root)){throw "Tiến trình $processId không còn là bản VetClinic đã khởi động; không dừng."}
 Stop-Process -Id $processId -ErrorAction Stop
}
Write-Output 'Đã dừng frontend/backend VetClinic. PostgreSQL và dữ liệu vẫn được giữ nguyên.'
