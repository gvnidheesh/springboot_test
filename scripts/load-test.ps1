<#
.SYNOPSIS
    Simple concurrent HTTP load test for the /load endpoint.

.PARAMETER Url
    Full URL of the endpoint to hit. Defaults to the local app on port 9080.

.PARAMETER TotalRequests
    Total number of requests to send.

.PARAMETER Concurrency
    Number of requests to have in flight at once.

.EXAMPLE
    ./scripts/load-test.ps1
    ./scripts/load-test.ps1 -Url http://localhost:9080/load -TotalRequests 500 -Concurrency 50
#>
param(
    [string]$Url = "http://localhost:9080/load",
    [int]$TotalRequests = 200,
    [int]$Concurrency = 20
)

Write-Host "Load testing $Url"
Write-Host "Total requests: $TotalRequests, Concurrency: $Concurrency"
Write-Host ""

$pool = [runspacefactory]::CreateRunspacePool(1, $Concurrency)
$pool.Open()

$scriptBlock = {
    param($Url)
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 30
        $sw.Stop()
        [pscustomobject]@{
            Success    = $true
            StatusCode = $response.StatusCode
            ElapsedMs  = $sw.Elapsed.TotalMilliseconds
            Error      = $null
        }
    } catch {
        $sw.Stop()
        $statusCode = $null
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
        }
        [pscustomobject]@{
            Success    = $false
            StatusCode = $statusCode
            ElapsedMs  = $sw.Elapsed.TotalMilliseconds
            Error      = $_.Exception.Message
        }
    }
}

$tasks = New-Object System.Collections.Generic.List[object]

$overallSw = [System.Diagnostics.Stopwatch]::StartNew()

for ($i = 0; $i -lt $TotalRequests; $i++) {
    $ps = [powershell]::Create()
    $ps.RunspacePool = $pool
    [void]$ps.AddScript($scriptBlock).AddArgument($Url)
    $handle = $ps.BeginInvoke()
    $tasks.Add([pscustomobject]@{ PS = $ps; Handle = $handle })
}

$results = New-Object System.Collections.Generic.List[object]
foreach ($task in $tasks) {
    $results.Add(($task.PS.EndInvoke($task.Handle) | Select-Object -First 1))
    $task.PS.Dispose()
}

$overallSw.Stop()
$pool.Close()
$pool.Dispose()

$success = $results | Where-Object { $_.Success }
$failed = $results | Where-Object { -not $_.Success }
$times = $results | ForEach-Object { $_.ElapsedMs }

Write-Host "=== Results ==="
Write-Host ("Total time:      {0:N2} s" -f ($overallSw.Elapsed.TotalSeconds))
Write-Host ("Requests/sec:    {0:N2}" -f ($TotalRequests / $overallSw.Elapsed.TotalSeconds))
Write-Host ("Success:         {0} / {1}" -f $success.Count, $TotalRequests)
Write-Host ("Failed:          {0}" -f $failed.Count)
if ($times.Count -gt 0) {
    Write-Host ("Latency min/avg/max (ms): {0:N1} / {1:N1} / {2:N1}" -f (
        ($times | Measure-Object -Minimum).Minimum,
        ($times | Measure-Object -Average).Average,
        ($times | Measure-Object -Maximum).Maximum
    ))
}
if ($failed.Count -gt 0) {
    Write-Host ""
    Write-Host "Sample errors:"
    $failed | Select-Object -First 5 | ForEach-Object { Write-Host "  $($_.Error)" }
}
