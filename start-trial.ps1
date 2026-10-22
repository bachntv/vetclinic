param([string]$Java='java',[string]$Node='node')
$ErrorActionPreference='Stop'
$root=$PSScriptRoot
$backend=Join-Path $root 'vetclinic'
$frontend=Join-Path $root 'frontend'
$state=Join-Path $root '.trial'
New-Item -ItemType Directory -Force $state | Out-Null
$jar=Join-Path $backend 'target/vetclinic-0.0.1-SNAPSHOT.jar'
if(!(Test-Path $jar)){throw 'Chạy .\mvnw.cmd package tại thư mục vetclinic trước.'}
if(!(Test-Path (Join-Path $frontend 'node_modules/vite/bin/vite.js'))){throw 'Chạy npm.cmd ci tại thư mục frontend trước.'}
$credentials=Join-Path $state 'credentials.json'
if(!(Test-Path $credentials)){
    $bytes=New-Object byte[] 24
    [Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
    $password=[Convert]::ToBase64String($bytes).Replace('+','A').Replace('/','B').TrimEnd('=')
    $bytes2=New-Object byte[] 48
    [Security.Cryptography.RandomNumberGenerator]::Fill($bytes2)
    @{email='trial@vetclinic.local';password=$password;jwtSecret=[Convert]::ToBase64String($bytes2)} | ConvertTo-Json | Set-Content $credentials -Encoding utf8
}
$credential=Get-Content -Raw $credentials | ConvertFrom-Json
# Values are inherited by the new Java process; they are not embedded into frontend assets.
$env:BOOTSTRAP_ADMIN_EMAIL=$credential.email
$env:BOOTSTRAP_ADMIN_PASSWORD=$credential.password
$env:BOOTSTRAP_ADMIN_FULL_NAME='Quản trị thử nghiệm'
$env:JWT_SECRET=$credential.jwtSecret
$api=Start-Process -FilePath $Java -WindowStyle Hidden -WorkingDirectory $backend -ArgumentList @('-jar', $jar,'--spring.profiles.active=demo') -RedirectStandardOutput (Join-Path $state 'backend.log') -RedirectStandardError (Join-Path $state 'backend-error.log') -PassThru
Remove-Item Env:BOOTSTRAP_ADMIN_PASSWORD,Env:JWT_SECRET
$ui=Start-Process -FilePath $Node -WindowStyle Hidden -WorkingDirectory $frontend -ArgumentList @('node_modules/vite/bin/vite.js','--mode','demo','--host','127.0.0.1','--port','15173','--strictPort') -RedirectStandardOutput (Join-Path $state 'frontend.log') -RedirectStandardError (Join-Path $state 'frontend-error.log') -PassThru
@{backendPid=$api.Id;frontendPid=$ui.Id;url='http://127.0.0.1:15173';startedAt=(Get-Date -Format o)} | ConvertTo-Json | Set-Content (Join-Path $state 'processes.json') -Encoding utf8
Write-Output 'Đã khởi động tiến trình thử nghiệm. Kiểm tra backend.log trước khi đăng nhập.'
Write-Output 'Giao diện: http://127.0.0.1:15173'
Write-Output "Tài khoản thử nghiệm nằm tại $credentials"
