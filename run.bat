@echo off
setlocal
cd /d "%~dp0"
set "FLATLAF_VERSION=3.7.1"
set "FLATLAF_JAR=lib\flatlaf-%FLATLAF_VERSION%.jar"
set "FLATLAF_URL=https://repo1.maven.org/maven2/com/formdev/flatlaf/%FLATLAF_VERSION%/flatlaf-%FLATLAF_VERSION%.jar"

if "%~1"=="" (
  where wscript >nul 2>nul
  if not errorlevel 1 (
    wscript //nologo "%~dp0LinovaOneERP.vbs"
    exit /b 0
  )
)

where javac >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Java JDK was not found.
  echo Please install JDK 8 or newer and make sure javac is available in PATH.
  pause
  exit /b 1
)

if not exist build\classes mkdir build\classes
if exist build\sources.txt del build\sources.txt

for /r src\main\java %%f in (*.java) do echo %%f>>build\sources.txt

echo Compiling Linova One ERP...
javac -encoding UTF-8 -cp "lib\*" -d build\classes @build\sources.txt
if errorlevel 1 (
  echo.
  echo [ERROR] Compile failed.
  pause
  exit /b 1
)

if "%~1"=="--compile-only" (
  echo Compile finished.
  exit /b 0
)

if "%~1"=="--init-db" (
  echo Initializing Linova One ERP database...
  java -cp "build\classes;lib\*" com.lin.erp.db.DatabaseSetup
  if errorlevel 1 (
    echo.
    echo [ERROR] Database initialization failed.
    pause
    exit /b 1
  )
  echo Database initialization finished.
  exit /b 0
)

if "%~1"=="--diagnose-login" (
  echo Diagnosing Linova One ERP login...
  if "%~2"=="" (
    java -cp "build\classes;lib\*" com.lin.erp.Diagnostics --login admin admin123
  ) else (
    java -cp "build\classes;lib\*" com.lin.erp.Diagnostics --login %2 %3
  )
  if errorlevel 1 exit /b 1
  exit /b 0
)

if "%~1"=="" call :ensure_flatlaf

echo Starting Linova One ERP...
where javaw >nul 2>nul
if errorlevel 1 (
  java -cp "build\classes;lib\*" com.lin.erp.ErpApp
) else (
  start "" javaw -cp "build\classes;lib\*" com.lin.erp.ErpApp
)

endlocal
exit /b 0

:ensure_flatlaf
if exist "%FLATLAF_JAR%" exit /b 0
if not exist lib mkdir lib
echo Downloading FlatLaf %FLATLAF_VERSION%...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -UseBasicParsing -Uri '%FLATLAF_URL%' -OutFile '%FLATLAF_JAR%'"
if errorlevel 1 (
  echo [WARN] FlatLaf could not be downloaded. The system look and feel will be used.
)
exit /b 0
