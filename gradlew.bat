@rem
@rem Self-contained Gradle wrapper for Windows.
@rem
@rem Like gradlew, this performs gradle-wrapper.jar's job itself rather than delegating to it, so no
@rem jar and no globally installed Gradle is required. Needs PowerShell once, on first run only.
@rem
@rem After the first successful run, do this:
@rem
@rem     gradlew.bat wrapper
@rem
@rem which regenerates the canonical gradlew, gradlew.bat and gradle-wrapper.jar from the
@rem distribution just installed. Recommended; changes no behaviour.
@rem

@if "%DEBUG%"=="" @echo off
setlocal enabledelayedexpansion

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.\
set PROPERTIES=%DIRNAME%gradle\wrapper\gradle-wrapper.properties

@rem ---- Java ------------------------------------------------------------------------------------
if defined JAVA_HOME (
    set JAVA_EXE=%JAVA_HOME%\bin\java.exe
    if not exist "!JAVA_EXE!" (
        echo.
        echo ERROR: JAVA_HOME points at an invalid directory: %JAVA_HOME%
        echo.
        exit /b 1
    )
) else (
    set JAVA_EXE=java.exe
    "!JAVA_EXE!" -version >NUL 2>&1
    if errorlevel 1 (
        echo.
        echo ERROR: No Java found. Install JDK 17 and set JAVA_HOME or put java on your PATH.
        echo This project targets Java 17. JDK 21 produces errors that look like Kotlin problems.
        echo.
        exit /b 1
    )
)

if not exist "%PROPERTIES%" (
    echo.
    echo ERROR: Missing %PROPERTIES%
    echo.
    exit /b 1
)

@rem ---- Distribution ----------------------------------------------------------------------------
for /f "tokens=1,* delims==" %%a in ('findstr /B "distributionUrl=" "%PROPERTIES%"') do set DIST_URL=%%b
set DIST_URL=%DIST_URL:\:=:%
for /f "tokens=1,* delims==" %%a in ('findstr /B "distributionPath=" "%PROPERTIES%"') do set DIST_PATH=%%b
if "%DIST_PATH%"=="" set DIST_PATH=wrapper/dists
set DIST_PATH=%DIST_PATH:/=\%

if "%GRADLE_USER_HOME%"=="" set GRADLE_USER_HOME=%USERPROFILE%\.gradle

for %%F in ("%DIST_URL%") do set ARCHIVE_NAME=%%~nxF
for %%F in ("%DIST_URL%") do set BASE_NAME=%%~nF
set BASE_NAME=%BASE_NAME:-bin=%
set BASE_NAME=%BASE_NAME:-all=%

set INSTALL_DIR=%GRADLE_USER_HOME%\%DIST_PATH%\%BASE_NAME%\shell-wrapper
set MARKER=%INSTALL_DIR%\.installed

if exist "%MARKER%" goto findHome

if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"
set ARCHIVE=%INSTALL_DIR%\%ARCHIVE_NAME%

if exist "%ARCHIVE%" goto unpack
echo Downloading %DIST_URL%
powershell -NoProfile -Command "$ProgressPreference='SilentlyContinue'; try { Invoke-WebRequest -Uri '%DIST_URL%' -OutFile '%ARCHIVE%.part' -UseBasicParsing; Move-Item -Force '%ARCHIVE%.part' '%ARCHIVE%' } catch { exit 1 }"
if not exist "%ARCHIVE%" (
    echo.
    echo ERROR: Download failed: %DIST_URL%
    echo.
    echo If you are offline or behind a proxy, download that file by hand, place it at
    echo   %ARCHIVE%
    echo and run this script again.
    echo.
    exit /b 1
)

:unpack
echo Unpacking to %INSTALL_DIR%
powershell -NoProfile -Command "$ProgressPreference='SilentlyContinue'; try { Expand-Archive -Path '%ARCHIVE%' -DestinationPath '%INSTALL_DIR%' -Force } catch { exit 1 }"
if errorlevel 1 (
    echo ERROR: Could not unpack %ARCHIVE%
    exit /b 1
)
del /q "%ARCHIVE%"
type nul > "%MARKER%"

:findHome
set GRADLE_HOME=
for /d %%d in ("%INSTALL_DIR%\*") do (
    if exist "%%d\bin\gradle.bat" set GRADLE_HOME=%%d
)
if "%GRADLE_HOME%"=="" (
    echo.
    echo ERROR: Gradle installation at %INSTALL_DIR% looks corrupt.
    echo Delete that directory and run this script again to re-download.
    echo.
    exit /b 1
)

call "%GRADLE_HOME%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
