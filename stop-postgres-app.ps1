$ErrorActionPreference='Stop'
$root=$PSScriptRoot
$apiMarker=[IO.Path]::GetFullPath((Join-Path $root 'vetclinic/target/vetclinic-0.0.1-SNAPSHOT.jar'))
$uiMarker=[IO.Path]::GetFullPath((Join-Path $root 'frontend/node_modules/vite/bin/vite.js'))
# Find the actual Java child; saved launcher PIDs may be stale.
$processes=Get-CimInstance Win32_Process|Where-Object {
 if(!$_.CommandLine){return $false}
 $line=$_.CommandLine.Replace('/','\')
 return (($_.Name -eq 'java.exe' -and $line.Contains($apiMarker.Replace('/','\'))) -or ($_.Name -eq 'node.exe' -and $line.Contains($uiMarker.Replace('/','\'))))
}
foreach($process in $processes){Stop-Process -Id $process.ProcessId -ErrorAction Stop}
Write-Output 'Đã dừng frontend/backend VetClinic. PostgreSQL và dữ liệu vẫn được giữ nguyên.'