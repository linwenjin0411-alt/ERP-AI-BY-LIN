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

    if (-not [string]::IsNullOrWhiteSpace($RuntimePath) -and -not (Test-Path -LiteralPath $RuntimePath)) {
        throw "RuntimePath does not exist: $RuntimePath"
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
    Copy-Item -LiteralPath (Join-Path $rootDir "config\license.properties.example") -Destination (Join-Path $distDir "config\license.properties.example") -Force
    Copy-Item -LiteralPath (Join-Path $rootDir "database\schema.mysql.sql") -Destination (Join-Path $distDir "database\schema.mysql.sql") -Force

    if (-not [string]::IsNullOrWhiteSpace($RuntimePath)) {
        Copy-Item -LiteralPath $RuntimePath -Destination (Join-Path $distDir "runtime") -Recurse -Force
    }

    @"
Linova One ERP delivery package

Start:
  Double-click LinovaOneERP.exe

First run:
  Double-click LinovaOneERP.exe. The startup script creates config\db.properties
  and config\license.properties from the bundled templates when they are missing.
  With the bundled default db.enabled=false, the app starts in local demo mode and
  does not connect to MySQL.

Database deployment:
  1. Edit config\db.properties and set db.enabled=true plus real database values.
  2. Edit config\license.properties and set the HTTPS license verification API.
  3. Run run.bat --init-db and set the initial administrator password.
  4. Sign in with the database administrator and register the online license key.

Optional bundled runtime:
  Pass -RuntimePath to build-distribution.ps1 to include a Windows JRE under runtime\.
  Packages without runtime\ require a compatible Java installation on the workstation.

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
