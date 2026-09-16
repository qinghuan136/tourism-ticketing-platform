param(
    [string]$ServerAddress = "http://localhost:8848",
    [string]$Group = "TOURISM",
    [string]$AccessToken = $env:NACOS_ACCESS_TOKEN
)

$configDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$headers = @{}
if ($AccessToken) {
    $headers["accessToken"] = $AccessToken
}

Get-ChildItem -LiteralPath $configDirectory -Filter '*.yml' | ForEach-Object {
    $content = [System.IO.File]::ReadAllText($_.FullName)
    $body = @{
        dataId = $_.Name
        groupName = $Group
        content = $content
        type = 'yaml'
    }

    Invoke-RestMethod -Method Post `
        -Uri "$($ServerAddress.TrimEnd('/'))/nacos/v3/admin/cs/config" `
        -Headers $headers `
        -ContentType 'application/x-www-form-urlencoded' `
        -Body $body `
        -ErrorAction Stop | Out-Null
    Write-Output "published=$Group/$($_.Name)"
}
