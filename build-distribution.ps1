param(
    [string] $OutputRoot = "",
    [string] $RuntimePath = ""
)

$ErrorActionPreference = "Stop"
$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path

if ([string]::IsNullOrWhiteSpace($OutputRoot)) {
    $OutputRoot = Join-Path $rootDir "build\dist"
}

$distDir = Join-Path $OutputRoot "LinovaOneERP"
$jarPath = Join-Path $rootDir "target\linova-one-erp.jar"
$launcherPath = Join-Path $rootDir "LinovaOneERP.exe"

Push-Location $rootDir
try {
    & cmd /c run.bat --compile-only
    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed."
    }

    & powershell -NoProfile -ExecutionPolicy Bypass -File .\build-launcher.ps1
    if ($LASTEXITCODE -ne 0) {
        throw "Launcher build failed."
    }

    if (Test-Path -LiteralPath $distDir) {
        Remove-Item -LiteralPath $distDir -Recurse -Force
    }

    New-Item -ItemType Directory -Force -Path $distDir | Out-Null
    New-Item -ItemType Directory -Force -Path (Join-Path $distDir "target") | Out-Null
    New-Item -ItemType Directory -Force -Path (Join-Path $distDir "config") | Out-Null
    New-Item -ItemType Directory -Force -Path (Join-Path $distDir "database") | Out-Null
    New-Item -ItemType Directory -Force -Path (Join-Path $distDir "logs") | Out-Null
    New-Item -ItemType Directory -Force -Path (Join-Path $distDir "exports") | Out-Null

    Copy-Item -LiteralPath $launcherPath -Destination (Join-Path $distDir "LinovaOneERP.exe") -Force
    Copy-Item -LiteralPath (Join-Path $rootDir "run.bat") -Destination (Join-Path $distDir "run.bat") -Force
    Copy-Item -LiteralPath $jarPath -Destination (Join-Path $distDir "target\linova-one-erp.jar") -Force
    Copy-Item -LiteralPath (Join-Path $rootDir "config\db.properties.example") -Destination (Join-Path $distDir "config\db.properties.example") -Force
    Copy-Item -LiteralPath (Join-Path $rootDir "database\schema.mysql.sql") -Destination (Join-Path $distDir "database\schema.mysql.sql") -Force

    if (-not [string]::IsNullOrWhiteSpace($RuntimePath)) {
        if (-not (Test-Path -LiteralPath $RuntimePath)) {
            throw "RuntimePath does not exist: $RuntimePath"
        }
        Copy-Item -LiteralPath $RuntimePath -Destination (Join-Path $distDir "runtime") -Recurse -Force
    }

    @"
Linova One ERP delivery package

Start:
  Double-click LinovaOneERP.exe

Required before first run:
  1. Copy config\db.properties.example to config\db.properties.
  2. Fill in real database connection values.
  3. Run run.bat --init-db.
  4. Enter/register a signed license key when prompted.

Optional bundled runtime:
  Put a Windows JRE under runtime\ or pass -RuntimePath to build-distribution.ps1.

Do not commit:
  config\db.properties
  config\license.properties
  logs\
  exports\
"@ | Set-Content -LiteralPath (Join-Path $distDir "README_DELIVERY.txt") -Encoding UTF8

    Write-Host "Distribution package created: $distDir"
} finally {
    Pop-Location
}
