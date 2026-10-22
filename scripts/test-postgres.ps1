param([switch]$Offline)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$backend=Join-Path $root 'vetclinic'
$docker=(Get-Command docker -ErrorAction Stop).Source
$container=& $docker compose -f (Join-Path $backend 'compose.yaml') ps -q postgres
if($LASTEXITCODE -ne 0 -or !$container){throw 'Khởi động PostgreSQL trước: docker compose up -d trong thư mục vetclinic.'}
$container=($container|Select-Object -First 1).Trim()
$info=(& $docker inspect $container|ConvertFrom-Json)[0]
$entry=$info.Config.Env|Where-Object {$_ -like 'POSTGRES_PASSWORD=*'}|Select-Object -First 1
if(!$entry){throw 'Không xác định được thông tin kết nối PostgreSQL.'}
$port=($info.NetworkSettings.Ports.'5432/tcp'|Where-Object {$_.HostIp -ne '::'}|Select-Object -First 1).HostPort
if(!$port){throw 'PostgreSQL chưa có cổng kết nối trên máy.'}
$suffix=Get-Date -Format 'yyyyMMdd_HHmmss'
$mainDatabase="vetclinic_test_$suffix"
$workflowDatabase="vetclinic_workflow_$suffix"
foreach($database in @($mainDatabase,$workflowDatabase)){
 if($database -notmatch '^vetclinic_(test|workflow)_[0-9_]+$'){throw 'Tên database kiểm thử không hợp lệ.'}
 & $docker exec $container createdb -U postgres $database
 if($LASTEXITCODE -ne 0){throw "Không tạo được database kiểm thử riêng $database"}
}
# Tests use create-drop only in the two newly created databases, never the operational database.
$variables=@{
 TEST_DATABASE_URL="jdbc:postgresql://127.0.0.1:${port}/${mainDatabase}";
 WORKFLOW_TEST_DATABASE_URL="jdbc:postgresql://127.0.0.1:${port}/${workflowDatabase}";
 TEST_DATABASE_USERNAME='postgres';TEST_DATABASE_PASSWORD=$entry.Substring('POSTGRES_PASSWORD='.Length)
}
$prior=@{}
Push-Location $backend
try{
 foreach($key in $variables.Keys){$prior[$key]=[Environment]::GetEnvironmentVariable($key,'Process');[Environment]::SetEnvironmentVariable($key,$variables[$key],'Process')}
 $arguments=@('test')
 if($Offline){$arguments=@('-o')+$arguments}
 & (Join-Path $backend 'mvnw.cmd') @arguments
 $testExit=$LASTEXITCODE
 if($testExit -ne 0){throw "Kiểm thử PostgreSQL thất bại, mã lỗi $testExit"}
 Write-Output "Đã kiểm thử trên PostgreSQL: $mainDatabase và $workflowDatabase. Database vận hành không bị dùng để chạy test."
}finally{
 foreach($key in $prior.Keys){[Environment]::SetEnvironmentVariable($key,$prior[$key],'Process')}
 Pop-Location
}
