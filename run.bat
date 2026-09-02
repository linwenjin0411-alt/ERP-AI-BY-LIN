@echo off
setlocal
cd /d "%~dp0"
set "APP_JAR=target\linova-one-erp.jar"
set "MAVEN_REPO=build\maven-repository"
set "JAVA_CMD=java"
set "JAVAW_CMD=javaw"

if exist "runtime\bin\java.exe" set "JAVA_CMD=%CD%\runtime\bin\java.exe"
if exist "runtime\bin\javaw.exe" set "JAVAW_CMD=%CD%\runtime\bin\javaw.exe"

if /i "%~1"=="--compile-only" (
  call :build_app
  if errorlevel 1 exit /b 1
  echo Compile finished.
  exit /b 0
)

call :ensure_app_jar
if errorlevel 1 exit /b 1

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

echo Starting Linova One ERP...
if exist "%JAVAW_CMD%" (
  start "" "%JAVAW_CMD%" -jar "%APP_JAR%"
  endlocal
  exit /b 0
)

where javaw >nul 2>nul
if errorlevel 1 (
  "%JAVA_CMD%" -jar "%APP_JAR%"
) else (
  start "" javaw -jar "%APP_JAR%"
)

endlocal
exit /b 0

:ensure_app_jar
if exist "%APP_JAR%" exit /b 0
echo [ERROR] Application jar was not found: %APP_JAR%
echo Run run.bat --compile-only on a build machine before starting the ERP client.
exit /b 1

:build_app
where mvn >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Apache Maven was not found.
  echo Install Maven 3.8 or newer, then run this command again.
  echo Maven downloads FlatLaf and MySQL Connector/J automatically; jar files are not committed.
  exit /b 1
)

echo Building Linova One ERP with Maven...
if not exist build mkdir build
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
exit /b 0
