param([string]$Java='java',[string]$Node='node',[int]$BackendPort=8080,[int]$FrontendPort=5173)
$ErrorActionPreference='Stop'
$root=$PSScriptRoot
$backend=Join-Path $root 'vetclinic'
$frontend=Join-Path $root 'frontend'
$state=Join-Path $root '.runtime'
$jar=Join-Path $backend 'target/vetclinic-0.0.1-SNAPSHOT.jar'
if(!(Test-Path -LiteralPath $jar)){throw 'Chưa có bản build backend. Chạy .\mvnw.cmd package trong thư mục vetclinic.'}
if(!(Test-Path -LiteralPath (Join-Path $frontend 'dist/index.html'))){throw 'Chưa có bản build frontend. Chạy npm.cmd run build trong thư mục frontend.'}
$docker=(Get-Command docker -ErrorAction Stop).Source
foreach($port in @($BackendPort,$FrontendPort)){
 if(Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue){throw "Cổng $port đang được sử dụng. Không khởi động thêm bản thứ hai."}
}
& $docker compose -f (Join-Path $backend 'compose.yaml') start postgres
if($LASTEXITCODE -ne 0){throw 'Không khởi động được container PostgreSQL hiện có.'}
$container=& $docker compose -f (Join-Path $backend 'compose.yaml') ps -q postgres
if($LASTEXITCODE -ne 0 -or !$container){throw 'Không tìm thấy container PostgreSQL.'}
$container=($container|Select-Object -First 1).Trim()
$ready=$false
for($attempt=0;$attempt -lt 30;$attempt++){
 & $docker exec $container pg_isready -U postgres -d vetclinic *> $null
 if($LASTEXITCODE -eq 0){$ready=$true;break}
 Start-Sleep -Seconds 1
}
if(!$ready){throw 'PostgreSQL chưa sẵn sàng sau 30 giây.'}
$info=(& $docker inspect $container|ConvertFrom-Json)[0]
$passwordEntry=$info.Config.Env|Where-Object {$_ -like 'POSTGRES_PASSWORD=*'}|Select-Object -First 1
if(!$passwordEntry){throw 'Không xác định được mật khẩu PostgreSQL từ cấu hình container.'}
$dbPassword=$passwordEntry.Substring('POSTGRES_PASSWORD='.Length)
$portEntry=$info.NetworkSettings.Ports.'5432/tcp'|Where-Object {$_.HostIp -ne '::'}|Select-Object -First 1
if(!$portEntry){throw 'PostgreSQL chưa được ánh xạ ra cổng trên máy.'}
$dbPort=$portEntry.HostPort
New-Item -ItemType Directory -Force $state|Out-Null
$credentials=Join-Path $state 'postgres-access.json'
if(!(Test-Path -LiteralPath $credentials)){
 $bytes=New-Object byte[] 24
 $rng=[Security.Cryptography.RandomNumberGenerator]::Create(); try{$rng.GetBytes($bytes)}finally{$rng.Dispose()}
 $password=[Convert]::ToBase64String($bytes).Replace('+','A').Replace('/','B').TrimEnd('=')
 $jwtBytes=New-Object byte[] 48
 $rng=[Security.Cryptography.RandomNumberGenerator]::Create(); try{$rng.GetBytes($jwtBytes)}finally{$rng.Dispose()}
 @{email='local-admin-20261022@vetclinic.local';password=$password;jwtSecret=[Convert]::ToBase64String($jwtBytes)}|ConvertTo-Json|Set-Content -LiteralPath $credentials -Encoding utf8
}
$credential=Get-Content -Raw -LiteralPath $credentials|ConvertFrom-Json
$variables=@{
 DB_URL="jdbc:postgresql://127.0.0.1:$dbPort/vetclinic?ApplicationName=vetclinic-app";
 DB_USERNAME='postgres';DB_PASSWORD=$dbPassword;JWT_SECRET=$credential.jwtSecret;
 BOOTSTRAP_ADMIN_EMAIL=$credential.email;BOOTSTRAP_ADMIN_PASSWORD=$credential.password;
 BOOTSTRAP_ADMIN_FULL_NAME='Quản trị VetClinic';API_PROXY_TARGET="http://127.0.0.1:$BackendPort"
}
$prior=@{}
try{
 foreach($key in $variables.Keys){$prior[$key]=[Environment]::GetEnvironmentVariable($key,'Process');[Environment]::SetEnvironmentVariable($key,$variables[$key],'Process')}
 $api=Start-Process -FilePath $Java -WindowStyle Hidden -WorkingDirectory $backend -ArgumentList @('-jar',('"'+$jar+'"'),'--spring.profiles.active=postgres','--spring.jpa.hibernate.ddl-auto=validate','--server.address=127.0.0.1',"--server.port=$BackendPort") -RedirectStandardOutput (Join-Path $state 'postgres-backend.log') -RedirectStandardError (Join-Path $state 'postgres-backend-error.log') -PassThru
 $ui=Start-Process -FilePath $Node -WindowStyle Hidden -WorkingDirectory $frontend -ArgumentList @(('"'+(Join-Path $frontend 'node_modules/vite/bin/vite.js')+'"'),'preview','--host','127.0.0.1','--port',"$FrontendPort",'--strictPort') -RedirectStandardOutput (Join-Path $state 'postgres-frontend.log') -RedirectStandardError (Join-Path $state 'postgres-frontend-error.log') -PassThru
 @{backendPid=$api.Id;frontendPid=$ui.Id;database='vetclinic';engine='PostgreSQL';frontendUrl="http://127.0.0.1:$FrontendPort";backendUrl="http://127.0.0.1:$BackendPort";startedAt=(Get-Date -Format o)}|ConvertTo-Json|Set-Content -LiteralPath (Join-Path $state 'postgres-processes.json') -Encoding utf8
}finally{foreach($key in $prior.Keys){[Environment]::SetEnvironmentVariable($key,$prior[$key],'Process')}}
Write-Output "Đã khởi động VetClinic với PostgreSQL. Giao diện: http://127.0.0.1:$FrontendPort"
Write-Output "Tài khoản quản trị cục bộ: $credentials"
Write-Output "Nhật ký khởi động: $(Join-Path $state 'postgres-backend.log')"
