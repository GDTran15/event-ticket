param(
    [string]$DatabasePassword
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $projectRoot '.env'
if (Test-Path -LiteralPath $envPath) {
    Write-Output '.env already exists; existing settings were preserved.'
    exit 0
}

function New-RandomSecret {
    param([int]$Length = 32)
    $bytes = New-Object byte[] $Length
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($bytes) }
    finally { $generator.Dispose() }
    return [Convert]::ToBase64String($bytes)
}

if ([string]::IsNullOrWhiteSpace($DatabasePassword)) {
    $DatabasePassword = New-RandomSecret
}
if ($DatabasePassword -match '[\r\n$\\]' -or $DatabasePassword -match '^\s|\s$') {
    throw 'Use a database password without newlines, dollar signs, backslashes, or surrounding whitespace in .env.'
}
$jwtSecret = New-RandomSecret
@"
DB_NAME=eventhub
DB_USER=postgres
DB_PASSWORD=$DatabasePassword
DB_PORT=5433
PORT=8080
JWT_SECRET_BASE64=$jwtSecret
BOOTSTRAP_ADMIN_EMAIL=
BOOTSTRAP_ADMIN_PASSWORD=
"@ | Set-Content -LiteralPath $envPath -Encoding ascii
Write-Output 'Created .env with a random JWT signing key and local database settings. Keep this file private.'
