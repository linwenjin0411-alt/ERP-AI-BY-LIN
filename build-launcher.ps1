param(
    [string] $OutputPath = "",
    [string] $IconPath = ""
)

$ErrorActionPreference = "Stop"
$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$sourcePath = Join-Path $rootDir "launcher\LinovaOneERPLauncher.cs"
$defaultIconPath = Join-Path $rootDir "launcher\LinovaOneERP.ico"

if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path $rootDir "LinovaOneERP.exe"
}

if (-not (Test-Path -LiteralPath $sourcePath)) {
    throw "Missing launcher source: $sourcePath"
}

if ([string]::IsNullOrWhiteSpace($IconPath) -and (Test-Path -LiteralPath $defaultIconPath)) {
    $IconPath = $defaultIconPath
}

if (Test-Path -LiteralPath $OutputPath) {
    Remove-Item -LiteralPath $OutputPath -Force
}

$source = Get-Content -LiteralPath $sourcePath -Raw -Encoding UTF8
$compilerParameters = New-Object System.CodeDom.Compiler.CompilerParameters
$compilerParameters.GenerateExecutable = $true
$compilerParameters.GenerateInMemory = $false
$compilerParameters.OutputAssembly = $OutputPath
$null = $compilerParameters.ReferencedAssemblies.Add("System.dll")
$null = $compilerParameters.ReferencedAssemblies.Add("System.Windows.Forms.dll")
$null = $compilerParameters.ReferencedAssemblies.Add("System.Drawing.dll")
if (-not [string]::IsNullOrWhiteSpace($IconPath)) {
    if (-not (Test-Path -LiteralPath $IconPath)) {
        throw "Missing launcher icon: $IconPath"
    }
    $compilerParameters.CompilerOptions = "/target:winexe /win32icon:`"$IconPath`""
} else {
    $compilerParameters.CompilerOptions = "/target:winexe"
}

Add-Type `
    -TypeDefinition $source `
    -CompilerParameters $compilerParameters

Write-Host "Built launcher: $OutputPath"
