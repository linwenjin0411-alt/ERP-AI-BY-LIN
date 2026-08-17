param(
    [string] $OutputPath = ""
)

$ErrorActionPreference = "Stop"
$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$sourcePath = Join-Path $rootDir "launcher\LinovaOneERPLauncher.cs"

if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path $rootDir "LinovaOneERP.exe"
}

if (-not (Test-Path -LiteralPath $sourcePath)) {
    throw "Missing launcher source: $sourcePath"
}

if (Test-Path -LiteralPath $OutputPath) {
    Remove-Item -LiteralPath $OutputPath -Force
}

$source = Get-Content -LiteralPath $sourcePath -Raw -Encoding UTF8
Add-Type `
    -TypeDefinition $source `
    -ReferencedAssemblies "System.Windows.Forms.dll", "System.Drawing.dll" `
    -OutputAssembly $OutputPath `
    -OutputType WindowsApplication

Write-Host "Built launcher: $OutputPath"
