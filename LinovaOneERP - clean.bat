@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo Cleaning Linova One ERP build output...
echo Please close Linova One ERP before running this script.

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop';" ^
  "$root=(Resolve-Path '.').Path;" ^
  "$targets=@('target','build\maven-status','build\maven-archiver');" ^
  "foreach($target in $targets) {" ^
  "  $path=Join-Path $root $target;" ^
  "  if(Test-Path -LiteralPath $path) {" ^
  "    $resolved=(Resolve-Path -LiteralPath $path).Path;" ^
  "    if(-not $resolved.StartsWith($root, [System.StringComparison]::OrdinalIgnoreCase)) { throw 'Refusing to clean outside workspace: ' + $resolved }" ^
  "    Remove-Item -LiteralPath $resolved -Recurse -Force;" ^
  "  }" ^
  "}"

if errorlevel 1 (
  echo.
  echo [ERROR] Clean failed. Close Linova One ERP and any java/javaw process that is using target\linova-one-erp.jar, then retry.
  pause
  exit /b 1
)

call run.bat --compile-only
if errorlevel 1 (
  echo.
  echo [ERROR] Rebuild failed. Check the Maven output above.
  pause
  exit /b 1
)

echo Starting Linova One ERP...
if /i "%~1"=="--no-start" (
  echo Clean rebuild finished. Startup was skipped by --no-start.
  endlocal
  exit /b 0
)

if exist "LinovaOneERP.exe" (
  start "" "LinovaOneERP.exe"
) else (
  call run.bat
)

endlocal
