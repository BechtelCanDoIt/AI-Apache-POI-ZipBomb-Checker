@echo off
REM ============================================================================
REM ZipBomb Evaluator - Automated Setup Script (Windows)
REM ============================================================================
REM This script:
REM   1. Checks prerequisites (Java 17+, Maven 3.8.1+)
REM   2. Creates the Maven project directory structure
REM   3. Places Java source files in correct locations
REM   4. Builds the project and creates the standalone JAR
REM ============================================================================

setlocal enabledelayedexpansion

echo.
echo ================================================================
echo          ZipBomb Evaluator - Setup Script v1.0.0
echo    Automated project setup for Windows
echo ================================================================
echo.

set SCRIPT_DIR=%~dp0
set ERRORS=0

REM ---- Step 1: Check prerequisites ----

echo [1/5] Checking prerequisites...

REM Check Java
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java is not installed or not in PATH.
    echo Please install Java 17 or later from https://adoptium.net
    exit /b 1
)

for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VER=%%~i
)
echo   Java detected: %JAVA_VER%

REM Check Maven
mvn -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Maven is not installed or not in PATH.
    echo Please install Maven 3.8.1+ from https://maven.apache.org/download.cgi
    exit /b 1
)
echo   Maven detected

REM ---- Step 2: Create directory structure ----

echo.
echo [2/5] Creating Maven project structure...

set MAIN_PKG=src\main\java\org\wso2\carbon\apimgt\impl\indexing
set TEST_PKG=src\test\java\org\wso2\carbon\apimgt\impl\indexing

if not exist "%SCRIPT_DIR%%MAIN_PKG%\util" mkdir "%SCRIPT_DIR%%MAIN_PKG%\util"
if not exist "%SCRIPT_DIR%%MAIN_PKG%\indexer" mkdir "%SCRIPT_DIR%%MAIN_PKG%\indexer"
if not exist "%SCRIPT_DIR%%TEST_PKG%\util" mkdir "%SCRIPT_DIR%%TEST_PKG%\util"
if not exist "%SCRIPT_DIR%src\main\resources" mkdir "%SCRIPT_DIR%src\main\resources"
if not exist "%SCRIPT_DIR%reference" mkdir "%SCRIPT_DIR%reference"

echo   Directory structure created

REM ---- Step 3: Place source files ----

echo.
echo [3/5] Placing source files...

if exist "%SCRIPT_DIR%ZipBombEvaluator.java" (
    copy /y "%SCRIPT_DIR%ZipBombEvaluator.java" "%SCRIPT_DIR%%MAIN_PKG%\util\ZipBombEvaluator.java" >nul
    echo   + ZipBombEvaluator.java
) else if not exist "%SCRIPT_DIR%%MAIN_PKG%\util\ZipBombEvaluator.java" (
    echo   ERROR: ZipBombEvaluator.java not found
    set /a ERRORS+=1
)

if exist "%SCRIPT_DIR%ZipBombEvaluatorMain.java" (
    copy /y "%SCRIPT_DIR%ZipBombEvaluatorMain.java" "%SCRIPT_DIR%%MAIN_PKG%\ZipBombEvaluatorMain.java" >nul
    echo   + ZipBombEvaluatorMain.java
) else if not exist "%SCRIPT_DIR%%MAIN_PKG%\ZipBombEvaluatorMain.java" (
    echo   ERROR: ZipBombEvaluatorMain.java not found
    set /a ERRORS+=1
)

if exist "%SCRIPT_DIR%DocumentIndexerSecurityFilter.java" (
    copy /y "%SCRIPT_DIR%DocumentIndexerSecurityFilter.java" "%SCRIPT_DIR%%MAIN_PKG%\indexer\DocumentIndexerSecurityFilter.java" >nul
    echo   + DocumentIndexerSecurityFilter.java
) else if not exist "%SCRIPT_DIR%%MAIN_PKG%\indexer\DocumentIndexerSecurityFilter.java" (
    echo   ERROR: DocumentIndexerSecurityFilter.java not found
    set /a ERRORS+=1
)

REM Handle test file (try both names)
if exist "%SCRIPT_DIR%ZipBombEvaluatorTest.java" (
    copy /y "%SCRIPT_DIR%ZipBombEvaluatorTest.java" "%SCRIPT_DIR%%TEST_PKG%\util\ZipBombEvaluatorTest.java" >nul
    echo   + ZipBombEvaluatorTest.java
) else if exist "%SCRIPT_DIR%ZipBombEvaluatorTest_Corrected.java" (
    copy /y "%SCRIPT_DIR%ZipBombEvaluatorTest_Corrected.java" "%SCRIPT_DIR%%TEST_PKG%\util\ZipBombEvaluatorTest.java" >nul
    echo   + ZipBombEvaluatorTest_Corrected.java (renamed)
) else if not exist "%SCRIPT_DIR%%TEST_PKG%\util\ZipBombEvaluatorTest.java" (
    echo   WARNING: Test file not found (non-fatal)
)

REM Reference files
if exist "%SCRIPT_DIR%DocumentIndexer_Modified_Example.java" (
    copy /y "%SCRIPT_DIR%DocumentIndexer_Modified_Example.java" "%SCRIPT_DIR%reference\" >nul
    echo   + DocumentIndexer_Modified_Example.java (reference)
)

if %ERRORS% gtr 0 (
    echo.
    echo ERROR: %ERRORS% required file(s) not found.
    echo Please ensure all Java source files are in the project root.
    exit /b 1
)

REM ---- Step 4: Verify pom.xml ----

echo.
echo [4/5] Verifying build configuration...

if not exist "%SCRIPT_DIR%pom.xml" (
    echo ERROR: pom.xml not found
    exit /b 1
)
echo   pom.xml found

REM ---- Step 5: Build ----

echo.
echo [5/5] Building project...
echo.

cd /d "%SCRIPT_DIR%"
call mvn clean package -q -DskipTests=false

if errorlevel 1 (
    echo.
    echo BUILD FAILED
    echo Run 'mvn clean compile -X' for detailed error output.
    exit /b 1
)

echo.
echo ================================================================
echo                     BUILD SUCCESSFUL!
echo ================================================================
echo.
echo Quick test:
echo   java -jar target\zipbomb-evaluator-1.0.0-standalone.jar --help
echo.
echo Evaluate a file:
echo   java -jar target\zipbomb-evaluator-1.0.0-standalone.jar file.xlsx
echo.

endlocal
