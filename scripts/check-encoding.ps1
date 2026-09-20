param(
    [string[]]$Roots = @("src", "config", "README.md", "README.zh-CN.md", "README.ja.md")
)

$ErrorActionPreference = "Stop"
$replacement = [char]0xFFFD
$c3 = [string]([char]0x00C3)
$c2 = [string]([char]0x00C2)
$e3 = [string]([char]0x00E3)
$a4 = [string]([char]0x00E4)
$a6 = [string]([char]0x00E6)
$patterns = @(
    $replacement,
    $c3,
    $c2,
    ($e3 + [string]([char]0x20AC)),
    ($e3 + [string]([char]0x0081)),
    ($e3 + [string]([char]0x0082)),
    ($e3 + [string]([char]0x0083)),
    ($a4 + [string]([char]0x00B8)),
    ($a6 + [string]([char]0x2014))
)
$extensions = @(".java", ".properties", ".md", ".txt", ".xml", ".bat")
$failed = $false

foreach ($root in $Roots) {
    if (-not (Test-Path -LiteralPath $root)) {
        continue
    }
    $item = Get-Item -LiteralPath $root
    $files = @()
    if ($item.PSIsContainer) {
        $files = Get-ChildItem -LiteralPath $item.FullName -Recurse -File |
            Where-Object { $extensions -contains $_.Extension.ToLowerInvariant() }
    } else {
        $files = @($item)
    }
    foreach ($file in $files) {
        $text = [System.IO.File]::ReadAllText($file.FullName, [System.Text.Encoding]::UTF8)
        foreach ($pattern in $patterns) {
            if ($text.Contains($pattern)) {
                Write-Error "Potential mojibake '$pattern' found in $($file.FullName)"
                $failed = $true
            }
        }
    }
}

if ($failed) {
    exit 1
}

Write-Host "Encoding check passed."
