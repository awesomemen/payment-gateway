param(
  [string]$BaseUrl = "http://localhost:18081",
  [string]$OutputDir
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($OutputDir)) {
  $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
  $OutputDir = Join-Path (Get-Location) ".tmp-reliability\$stamp"
}

$null = New-Item -ItemType Directory -Force -Path $OutputDir

$MerchantId = "MCH100001"
$SignatureKey = "demo-signature-key"
$MysqlContainer = "gateway-mysql"

function New-IsoInstantUtc {
  return [DateTime]::UtcNow.ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
}

function New-Signature(
    [string]$MerchantId,
    [string]$RequestId,
    [string]$IdempotencyKey,
    [string]$Amount,
    [string]$Currency,
    [string]$RequestTime,
    [string]$Nonce,
    [string]$Key
) {
  $payload = "merchantId=$MerchantId&requestId=$RequestId&idempotencyKey=$IdempotencyKey&amount=$Amount&currency=$Currency&requestTime=$RequestTime&nonce=$Nonce"
  $hmac = [System.Security.Cryptography.HMACSHA256]::new([Text.Encoding]::UTF8.GetBytes($Key))
  try {
    return ([System.BitConverter]::ToString($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($payload)))).Replace("-", "").ToLowerInvariant()
  } finally {
    $hmac.Dispose()
  }
}

function Invoke-JsonRequest([string]$Url, [string]$Method, [object]$Body) {
  $bodyText = if ($null -eq $Body) { $null } else { $Body | ConvertTo-Json -Depth 8 }
  $invokeWebRequest = Get-Command Invoke-WebRequest
  $requestArgs = @{
    Uri = $Url
    Method = $Method
    UseBasicParsing = $true
  }
  if ($invokeWebRequest.Parameters.ContainsKey("SkipHttpErrorCheck")) {
    $requestArgs.SkipHttpErrorCheck = $true
  }
  if ($null -ne $bodyText) {
    $requestArgs.ContentType = "application/json"
    $requestArgs.Body = $bodyText
  }
  $response = $null
  $contentText = $null
  try {
    $response = Invoke-WebRequest @requestArgs
  } catch [System.Net.WebException] {
    if ($null -eq $_.Exception.Response) {
      throw
    }
    $response = $_.Exception.Response
    $stream = $response.GetResponseStream()
    $reader = [System.IO.StreamReader]::new($stream)
    try {
      $contentText = $reader.ReadToEnd()
    } finally {
      $reader.Dispose()
      $stream.Dispose()
    }
  }
  if ($null -eq $contentText) {
    $contentText = if ($response.Content -is [byte[]]) {
      [Text.Encoding]::UTF8.GetString($response.Content)
    } else {
      [string]$response.Content
    }
  }
  $parsedBody = $null
  if (-not [string]::IsNullOrWhiteSpace($contentText)) {
    try {
      $parsedBody = $contentText | ConvertFrom-Json
    } catch {
      $parsedBody = $contentText
    }
  }
  return [pscustomobject]@{
    StatusCode = [int]$response.StatusCode
    BodyText = $contentText
    Body = $parsedBody
    RequestBody = $bodyText
  }
}

function Invoke-PaymentCreate([string]$Prefix) {
  $requestId = "$Prefix-$([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())"
  $idempotencyKey = "IDEMP-$Prefix-$([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())"
  $requestTime = New-IsoInstantUtc
  $nonce = "nonce-$([guid]::NewGuid().ToString('N').Substring(0, 12))"
  $amount = "88.5"
  $currency = "CNY"
  $signature = New-Signature $MerchantId $requestId $idempotencyKey $amount $currency $requestTime $nonce $SignatureKey
  $payload = @{
    merchantId = $MerchantId
    requestId = $requestId
    idempotencyKey = $idempotencyKey
    amount = 88.5
    currency = $currency
    requestTime = $requestTime
    nonce = $nonce
    signature = $signature
  }
  $result = Invoke-JsonRequest "$BaseUrl/api/v1/payments" "POST" $payload
  $result | Add-Member -NotePropertyName RequestId -NotePropertyValue $requestId
  $result | Add-Member -NotePropertyName IdempotencyKey -NotePropertyValue $idempotencyKey
  return $result
}

function Invoke-Health {
  return Invoke-JsonRequest "$BaseUrl/actuator/health" "GET" $null
}

function Invoke-OutboxRetry {
  return Invoke-JsonRequest "$BaseUrl/api/v1/messaging/outbox/retry" "POST" $null
}

function Get-ContainerHealth([string]$ContainerName) {
  $json = docker inspect --format='{{json .State}}' $ContainerName
  if ([string]::IsNullOrWhiteSpace($json)) {
    throw "Unable to inspect container $ContainerName"
  }
  return $json | ConvertFrom-Json -Depth 8
}

function Wait-ContainerHealthy([string]$ContainerName, [int]$TimeoutSeconds = 90) {
  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    $state = Get-ContainerHealth $ContainerName
    if ($state.Status -eq "running") {
      if ($null -eq $state.Health -or $state.Health.Status -eq "healthy") {
        return
      }
    }
    Start-Sleep -Seconds 2
  }
  throw "Container $ContainerName was not healthy within $TimeoutSeconds seconds"
}

function Wait-AppHealthy([int]$TimeoutSeconds = 180) {
  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  $attempt = 0
  while ((Get-Date) -lt $deadline) {
    $attempt++
    try {
      $health = Invoke-Health
      if ($health.StatusCode -eq 200 -and $health.Body.status -eq "UP") {
        return $health
      }
      if ($attempt -eq 1 -or ($attempt % 10) -eq 0) {
        Write-Output "Health probe attempt $attempt => HTTP $($health.StatusCode), status=$($health.Body.status)"
      }
    } catch {
      if ($attempt -eq 1 -or ($attempt % 10) -eq 0) {
        Write-Output "Health probe attempt $attempt => ERROR $($_.Exception.Message)"
      }
    }
    Start-Sleep -Seconds 2
  }
  throw "Application health did not recover within $TimeoutSeconds seconds"
}

function Invoke-MysqlQuery([string]$Sql) {
  return docker exec $MysqlContainer mysql -N -ugateway -pgateway_local_pass -D gateway_db -e $Sql
}

function Get-OutboxRecord([string]$GatewayPaymentId) {
  $rows = Invoke-MysqlQuery "select event_key, message_key, send_status, retry_count, ifnull(date_format(next_retry_time,'%Y-%m-%d %H:%i:%s.%f'),'NULL'), ifnull(last_error_message,'NULL') from gateway_mq_outbox where event_key='OUTBOX-$GatewayPaymentId';"
  $line = @($rows | Where-Object { -not [string]::IsNullOrWhiteSpace($_) -and $_ -notlike "mysql:*" }) | Select-Object -First 1
  if ([string]::IsNullOrWhiteSpace($line)) {
    return $null
  }
  $parts = ($line -split "`t", 6)
  return [pscustomobject]@{
    EventKey = $parts[0]
    MessageKey = $parts[1]
    SendStatus = [int]$parts[2]
    RetryCount = [int]$parts[3]
    NextRetryTime = $parts[4]
    LastErrorMessage = $parts[5]
  }
}

function Get-PaymentOrder([string]$RequestId) {
  $rows = Invoke-MysqlQuery "select gateway_payment_id, request_id, payment_status, route_code, target_service, downstream_payment_id from gateway_payment_order where request_id='$RequestId';"
  $line = @($rows | Where-Object { -not [string]::IsNullOrWhiteSpace($_) -and $_ -notlike "mysql:*" }) | Select-Object -First 1
  if ([string]::IsNullOrWhiteSpace($line)) {
    return $null
  }
  $parts = ($line -split "`t", 6)
  return [pscustomobject]@{
    GatewayPaymentId = $parts[0]
    RequestId = $parts[1]
    PaymentStatus = $parts[2]
    RouteCode = $parts[3]
    TargetService = $parts[4]
    DownstreamPaymentId = $parts[5]
  }
}

function Mark-OutboxDueUtc([string]$EventKey) {
  Invoke-MysqlQuery "update gateway_mq_outbox set next_retry_time = utc_timestamp(3) where event_key='$EventKey';" | Out-Null
}

function Save-Evidence([string]$Name, [object]$Data) {
  $path = Join-Path $OutputDir "$Name.json"
  $Data | ConvertTo-Json -Depth 20 | Set-Content -Path $path -Encoding UTF8
}

function New-ScenarioResult([string]$Name) {
  return [ordered]@{
    name = $Name
    startedAt = (Get-Date).ToUniversalTime().ToString("o")
    verdict = "REVIEW"
    notes = @()
  }
}

Write-Output "Checking initial application health..."
Wait-AppHealthy | Out-Null

$summary = [ordered]@{
  generatedAt = (Get-Date).ToUniversalTime().ToString("o")
  baseUrl = $BaseUrl
  outputDir = $OutputDir
  scenarios = @()
}

# Redis outage
Write-Output "Running redis-outage scenario..."
$redisScenario = New-ScenarioResult "redis-outage"
docker stop gateway-redis | Out-Null
Start-Sleep -Seconds 4
$redisResponse = Invoke-PaymentCreate "REQ-REDIS-DOWN"
$redisScenario.response = @{
  statusCode = $redisResponse.StatusCode
  body = $redisResponse.Body
}
$redisScenario.notes += "Redis down probe completed"
docker start gateway-redis | Out-Null
Wait-ContainerHealthy "gateway-redis"
Start-Sleep -Seconds 8
Write-Output "redis-outage scenario completed."
$redisScenario.verdict = if ($redisResponse.StatusCode -ge 500 -and $redisResponse.Body.code -eq "INTERNAL_ERROR") { "PASS" } else { "FAIL" }
$summary.scenarios += $redisScenario
Save-Evidence "redis-outage" $redisScenario

# MySQL outage
Write-Output "Running mysql-outage scenario..."
$mysqlScenario = New-ScenarioResult "mysql-outage"
docker stop gateway-mysql | Out-Null
Start-Sleep -Seconds 6
$mysqlResponse = Invoke-PaymentCreate "REQ-MYSQL-DOWN"
$mysqlScenario.response = @{
  statusCode = $mysqlResponse.StatusCode
  body = $mysqlResponse.Body
}
$mysqlScenario.notes += "MySQL down probe completed"
docker start gateway-mysql | Out-Null
Wait-ContainerHealthy "gateway-mysql" 120
Start-Sleep -Seconds 15
Write-Output "mysql-outage scenario completed."
$mysqlScenario.verdict = if ($mysqlResponse.StatusCode -ge 500 -and $mysqlResponse.Body.code -eq "INTERNAL_ERROR") { "PASS" } else { "FAIL" }
$summary.scenarios += $mysqlScenario
Save-Evidence "mysql-outage" $mysqlScenario

# RocketMQ broker outage with outbox recovery
Write-Output "Running rocketmq-broker-outage scenario..."
$brokerScenario = New-ScenarioResult "rocketmq-broker-outage"
docker stop gateway-rocketmq-broker | Out-Null
Start-Sleep -Seconds 4
$brokerResponse = Invoke-PaymentCreate "REQ-BROKER-DOWN"
$gatewayPaymentId = $brokerResponse.Body.data.gatewayPaymentId
$brokerScenario.createResponse = @{
  statusCode = $brokerResponse.StatusCode
  body = $brokerResponse.Body
}
docker start gateway-rocketmq-broker | Out-Null
Wait-ContainerHealthy "gateway-rocketmq-broker" 120
Start-Sleep -Seconds 8
$brokerScenario.paymentOrder = if ($brokerResponse.RequestId) { Get-PaymentOrder $brokerResponse.RequestId } else { $null }
$brokerScenario.outboxBeforeRetry = if ($gatewayPaymentId) { Get-OutboxRecord $gatewayPaymentId } else { $null }
$brokerScenario.retryResponse = $null
$brokerScenario.outboxAfterRetry = $null
if ($brokerScenario.outboxBeforeRetry -and $brokerScenario.outboxBeforeRetry.SendStatus -eq 2) {
  Mark-OutboxDueUtc $brokerScenario.outboxBeforeRetry.EventKey
  $retryResponse = Invoke-OutboxRetry
  $brokerScenario.retryResponse = @{
    statusCode = $retryResponse.StatusCode
    body = $retryResponse.Body
  }
  $brokerScenario.outboxAfterRetry = Get-OutboxRecord $gatewayPaymentId
}
$brokerScenario.verdict = if (
  $brokerResponse.StatusCode -eq 200 -and
  $brokerScenario.paymentOrder -and
  $brokerScenario.outboxBeforeRetry -and
  $brokerScenario.outboxBeforeRetry.SendStatus -eq 2 -and
  $brokerScenario.retryResponse.body.data.succeededCount -ge 1 -and
  $brokerScenario.outboxAfterRetry.SendStatus -eq 1
) { "PASS" } else { "FAIL" }
$brokerScenario.notes += "Broker down create should still accept and leave a failed outbox record recoverable after broker startup"
$summary.scenarios += $brokerScenario
Save-Evidence "rocketmq-broker-outage" $brokerScenario
Write-Output "rocketmq-broker-outage scenario completed."

# Seata outage
Write-Output "Running seata-outage scenario..."
$seataScenario = New-ScenarioResult "seata-outage"
docker stop gateway-seata | Out-Null
Start-Sleep -Seconds 4
$seataResponse = Invoke-PaymentCreate "REQ-SEATA-DOWN"
$seataScenario.createResponse = @{
  statusCode = $seataResponse.StatusCode
  body = $seataResponse.Body
}
docker start gateway-seata | Out-Null
Wait-ContainerHealthy "gateway-seata" 120
Start-Sleep -Seconds 10
$seataScenario.paymentOrder = if ($seataResponse.RequestId) { Get-PaymentOrder $seataResponse.RequestId } else { $null }
$seataScenario.verdict = if ($seataResponse.StatusCode -eq 200 -and $seataScenario.paymentOrder) { "REVIEW" } else { "FAIL" }
$seataScenario.notes += "Current observed behavior: request still succeeds and payment order persists while Seata is down; final policy still needs business/architecture confirmation"
$summary.scenarios += $seataScenario
Save-Evidence "seata-outage" $seataScenario
Write-Output "seata-outage scenario completed."

Write-Output "Checking final application health..."
$health = Wait-AppHealthy
$summary.finalHealth = @{
  statusCode = $health.StatusCode
  body = $health.Body
}

$summaryPath = Join-Path $OutputDir "summary.json"
$summary | ConvertTo-Json -Depth 20 | Set-Content -Path $summaryPath -Encoding UTF8

$markdown = @(
  "# Local Reliability Suite",
  "",
  "- generatedAt: $($summary.generatedAt)",
  "- baseUrl: $BaseUrl",
  "- outputDir: $OutputDir",
  "",
  "| Scenario | Verdict | Key Observation |",
  "| --- | --- | --- |"
)
foreach ($scenario in $summary.scenarios) {
  $observation = switch ($scenario.name) {
    "redis-outage" { "payment create => HTTP $($scenario.response.statusCode) / $($scenario.response.body.code)" }
    "mysql-outage" { "payment create => HTTP $($scenario.response.statusCode) / $($scenario.response.body.code)" }
    "rocketmq-broker-outage" { "create => HTTP $($scenario.createResponse.statusCode); outboxBefore=$($scenario.outboxBeforeRetry.SendStatus); outboxAfter=$($scenario.outboxAfterRetry.SendStatus)" }
    "seata-outage" { "payment create => HTTP $($scenario.createResponse.statusCode) / $($scenario.createResponse.body.code)" }
    default { "n/a" }
  }
  $markdown += "| $($scenario.name) | $($scenario.verdict) | $observation |"
}
$markdown += ""
$markdown += "Final health: $($summary.finalHealth.body.status)"
Set-Content -Path (Join-Path $OutputDir "summary.md") -Value $markdown -Encoding UTF8

Write-Output "Local reliability suite finished."
Write-Output "Evidence directory: $OutputDir"
Get-Content (Join-Path $OutputDir "summary.md")
