@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo Cleaning Linova One ERP build output...
echo Please close Linova One ERP before running this script.

:clean
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
  echo [ERROR] Clean failed because a generated file is still in use.
  echo Close every Linova One ERP window first. If no window is visible, close java.exe/javaw.exe from Task Manager.
  choice /C RQ /N /M "Press R to retry after closing it, or Q to quit: "
  if errorlevel 2 exit /b 1
  echo.
  echo Retrying clean...
  goto clean
)

if exist "target\linova-one-erp.jar" (
  echo.
  echo [ERROR] Clean did not remove target\linova-one-erp.jar.
  echo Close Linova One ERP and retry.
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
