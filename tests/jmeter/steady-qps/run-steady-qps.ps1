$ErrorActionPreference = 'Stop'

# 登录阶段不纳入 QPS；只为后续只读请求准备不同游客的 JWT。
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = (Resolve-Path (Join-Path $scriptDir '..\..\..')).Path
$jmeterBat = 'C:\apache-jmeter-5.6.3\bin\jmeter.bat'
$plan = Join-Path $scriptDir 'tourist-visitors-steady-qps.jmx'
$credentials = Join-Path $projectRoot 'tests\jmeter\coupon-seckill\users.csv'
$resultRoot = Join-Path $scriptDir 'results'
$hostName = 'localhost'
$port = 8081
$rampUpSeconds = 15
$steadySeconds = 60
$concurrencies = @(50, 100, 200)

if (-not (Test-Path -LiteralPath $jmeterBat)) { throw "JMeter not found: $jmeterBat" }
if (-not (Test-Path -LiteralPath $credentials)) { throw "Credentials not found: $credentials" }

$tcp = New-Object System.Net.Sockets.TcpClient
try {
    $connection = $tcp.BeginConnect($hostName, $port, $null, $null)
    if (-not $connection.AsyncWaitHandle.WaitOne(3000)) { throw 'Backend connection timed out.' }
    $tcp.EndConnect($connection)
} finally { $tcp.Dispose() }

$allCredentials = @(Get-Content -LiteralPath $credentials -Encoding UTF8 |
    ForEach-Object { $_.Trim() } |
    Where-Object { $_ -and -not $_.StartsWith('#') })
$maxConcurrency = ($concurrencies | Measure-Object -Maximum).Maximum
if ($allCredentials.Count -lt $maxConcurrency) { throw "At least $maxConcurrency accounts are required." }

$runDir = Join-Path $resultRoot (Get-Date -Format 'yyyyMMdd-HHmmss')
New-Item -ItemType Directory -Force -Path $runDir | Out-Null
$tokenPath = Join-Path $runDir 'tokens.csv'

# 令牌仅保存在本轮临时目录，测试结束后删除。
$tokens = foreach ($line in $allCredentials | Select-Object -First $maxConcurrency) {
    $parts = $line.Split(',', 2)
    $body = @{ loginName = $parts[0]; password = $parts[1] } | ConvertTo-Json -Compress
    $login = Invoke-RestMethod -Method Post -Uri "http://$hostName`:$port/auth/login" -ContentType 'application/json' -Body $body -TimeoutSec 10
    if ($login.code -ne 'SUCCESS' -or [string]::IsNullOrWhiteSpace($login.data)) { throw "Login failed for $($parts[0])" }
    $login.data
}
[System.IO.File]::WriteAllLines($tokenPath, [string[]]$tokens, [System.Text.UTF8Encoding]::new($false))

function Get-Percentile([int[]]$values, [double]$percentile) {
    if ($values.Count -eq 0) { return 0 }
    $sorted = @($values | Sort-Object)
    $index = [Math]::Ceiling($sorted.Count * $percentile) - 1
    return $sorted[[Math]::Max(0, $index)]
}

$rows = @()
try {
    foreach ($concurrency in $concurrencies) {
        $caseDir = Join-Path $runDir "concurrency-$concurrency"
        $jtl = Join-Path $caseDir 'result.jtl'
        $log = Join-Path $caseDir 'jmeter.log'
        New-Item -ItemType Directory -Force -Path $caseDir | Out-Null
        $duration = $rampUpSeconds + $steadySeconds

        & $jmeterBat -n -t $plan -l $jtl -j $log `
            "-Jhost=$hostName" "-Jport=$port" "-Jthreads=$concurrency" `
            "-Jramp_up_seconds=$rampUpSeconds" "-Jduration_seconds=$duration" `
            "-Jtokens_file=$tokenPath"
        if ($LASTEXITCODE -ne 0) { throw "JMeter failed for concurrency $concurrency. See $log" }

        $samples = @(Import-Csv -LiteralPath $jtl | Where-Object { $_.label -eq 'GET /tourist/visitors' })
        $start = [long](($samples | Measure-Object -Property timeStamp -Minimum).Minimum)
        $steadyStart = $start + $rampUpSeconds * 1000
        $steadyEnd = $steadyStart + $steadySeconds * 1000
        $steady = @($samples | Where-Object { [long]$_.timeStamp -ge $steadyStart -and [long]$_.timeStamp -lt $steadyEnd })
        $elapsed = @($steady | ForEach-Object { [int]$_.elapsed })
        $errors = @($steady | Where-Object { $_.success -ne 'true' }).Count

        $rows += [pscustomobject]@{
            Concurrency = $concurrency
            Samples = $steady.Count
            SteadyQps = [Math]::Round($steady.Count / $steadySeconds, 2)
            AvgMs = [Math]::Round((($elapsed | Measure-Object -Average).Average), 2)
            P95Ms = Get-Percentile $elapsed 0.95
            Errors = $errors
        }
    }
} finally {
    if (Test-Path -LiteralPath $tokenPath) { Remove-Item -LiteralPath $tokenPath -Force }
}

$rows | Format-Table -AutoSize
$rows | Export-Csv -LiteralPath (Join-Path $runDir 'summary.csv') -NoTypeInformation -Encoding utf8
$rows | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $runDir 'summary.json') -Encoding utf8
Write-Host "Results: $runDir"
