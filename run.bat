@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "APP_JAR=target\linova-one-erp.jar"
set "MAVEN_REPO=build\maven-repository"
set "JAVA_CMD=java"
set "JAVAW_CMD=javaw"
set "BUILD_ONLY=0"
set "NEED_BUILD=0"

if exist "runtime\bin\java.exe" set "JAVA_CMD=%CD%\runtime\bin\java.exe"
if exist "runtime\bin\javaw.exe" set "JAVAW_CMD=%CD%\runtime\bin\javaw.exe"

if /i "%~1"=="--compile-only" (
  set "BUILD_ONLY=1"
  set "NEED_BUILD=1"
)

if "%BUILD_ONLY%"=="0" (
  if not exist "config" mkdir "config"
  if not exist "config" (
    echo [ERROR] Failed to create config directory.
    exit /b 1
  )
  if not exist "config\db.properties" (
    if exist "config\db.properties.example" (
      copy /y "config\db.properties.example" "config\db.properties" >nul
      if errorlevel 1 (
        echo [ERROR] Failed to create config\db.properties.
        exit /b 1
      )
      echo Created config\db.properties from template.
    ) else (
      echo [ERROR] Missing config\db.properties.example.
      exit /b 1
    )
  )
  if not exist "config\license.properties" (
    if exist "config\license.properties.example" (
      copy /y "config\license.properties.example" "config\license.properties" >nul
      if errorlevel 1 (
        echo [ERROR] Failed to create config\license.properties.
        exit /b 1
      )
      echo Created config\license.properties from template.
    ) else (
      echo [ERROR] Missing config\license.properties.example.
      exit /b 1
    )
  )
)

if not exist "%APP_JAR%" (
  echo Application jar was not found. Building it now...
  set "NEED_BUILD=1"
)

if "%NEED_BUILD%"=="0" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$jar=(Get-Item -LiteralPath '%APP_JAR%').LastWriteTimeUtc; $paths=@('pom.xml','src','config\*.example'); $newer=Get-ChildItem -Path $paths -Recurse -File -ErrorAction SilentlyContinue | Where-Object { $_.LastWriteTimeUtc -gt $jar } | Select-Object -First 1; if ($newer) { exit 2 } else { exit 0 }"
  if errorlevel 2 (
    echo Source files changed after the application jar was built. Rebuilding it now...
    set "NEED_BUILD=1"
  ) else if errorlevel 1 (
    echo [WARN] Source freshness check failed. Continuing with existing jar.
  )
)

if "%NEED_BUILD%"=="0" (
  "%JAVA_CMD%" -cp "%APP_JAR%" com.lin.erp.ui.ExportDiagnostics --self-check >nul 2>nul
  if errorlevel 1 (
    echo Application jar is missing recent export runtime classes. Rebuilding it now...
    set "NEED_BUILD=1"
  )
)

if "%NEED_BUILD%"=="1" (
  if /i "%~1"=="--compile-only" (
    if exist "scripts\check-encoding.ps1" (
      powershell -NoProfile -ExecutionPolicy Bypass -File "scripts\check-encoding.ps1"
      if errorlevel 1 (
        echo.
        echo [ERROR] Encoding check failed.
        exit /b 1
      )
    )
  )

  where mvn >nul 2>nul
  if errorlevel 1 (
    echo [ERROR] Apache Maven was not found.
    echo Install Maven 3.8 or newer, then run this command again.
    echo Maven downloads FlatLaf and MySQL Connector/J automatically; jar files are not committed.
    exit /b 1
  )

  echo Building Linova One ERP with Maven...
  echo Please wait. First build may take a few minutes while dependencies are prepared.
  if not exist "build" mkdir "build"
  call mvn -q -DskipTests "-Dmaven.repo.local=%MAVEN_REPO%" package
  if errorlevel 1 (
    echo.
    echo [ERROR] Maven build failed.
    exit /b 1
  )

  if not exist "%APP_JAR%" (
    echo [ERROR] Built application jar was not found: %APP_JAR%
    exit /b 1
  )
)

if "%BUILD_ONLY%"=="1" (
  echo Compile finished.
  exit /b 0
)

if /i "%~1"=="--init-db" (
  echo Initializing Linova One ERP database...
  "%JAVA_CMD%" -cp "%APP_JAR%" com.lin.erp.db.DatabaseSetup
  if errorlevel 1 (
    echo.
    echo [ERROR] Database initialization failed.
    exit /b 1
  )
  echo Database initialization finished.
  exit /b 0
)

if /i "%~1"=="--diagnose-login" (
  echo Diagnosing Linova One ERP login...
  if "%~2"=="" (
    "%JAVA_CMD%" -cp "%APP_JAR%" com.lin.erp.Diagnostics --login admin admin123
  ) else (
    "%JAVA_CMD%" -cp "%APP_JAR%" com.lin.erp.Diagnostics --login "%~2" "%~3"
  )
  if errorlevel 1 exit /b 1
  exit /b 0
)

if /i "%~1"=="--diagnose-export" (
  echo Diagnosing Linova One ERP export...
  "%JAVA_CMD%" -cp "%APP_JAR%" com.lin.erp.ui.ExportDiagnostics --smoke
  if errorlevel 1 exit /b 1
  exit /b 0
)

echo Starting Linova One ERP...
if exist "%JAVAW_CMD%" (
  start "" "%JAVAW_CMD%" -jar "%APP_JAR%"
  endlocal
  exit /b 0
)

where javaw >nul 2>nul
if errorlevel 1 (
  start "" "%JAVA_CMD%" -jar "%APP_JAR%"
) else (
  start "" javaw -jar "%APP_JAR%"
)

endlocal
exit /b 0
