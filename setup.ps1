<#
  One-time setup for a freshly extracted copy.

  WHY THIS EXISTS
  ---------------
  `local.properties` holds the absolute path to your Android SDK. It is machine-specific and is in
  .gitignore for good reason — committing it breaks the build for everyone else. But that means
  every fresh extract of the project has no SDK path, and Gradle fails with:

      SDK location not found. Define a valid SDK location with an ANDROID_HOME environment
      variable or by setting the sdk.dir path in your project's local.properties file

  This script finds the SDK and writes the file. Run it once per extracted copy:

      .\setup.ps1

  BETTER: set ANDROID_HOME once, system-wide, and no extracted copy will ever need this again.
  The script offers to do that for you.
#>

$ErrorActionPreference = "Stop"

function Find-AndroidSdk {
    $candidates = @(
        $env:ANDROID_HOME,
        $env:ANDROID_SDK_ROOT,
        "$env:LOCALAPPDATA\Android\Sdk",
        "$env:USERPROFILE\AppData\Local\Android\Sdk",
        "C:\Android\Sdk"
    )
    foreach ($path in $candidates) {
        if ($path -and (Test-Path (Join-Path $path "platform-tools"))) { return $path }
    }
    return $null
}

$sdk = Find-AndroidSdk

if (-not $sdk) {
    Write-Host ""
    Write-Host "Could not find an Android SDK." -ForegroundColor Red
    Write-Host ""
    Write-Host "Install it via Android Studio (Settings -> Languages and Frameworks -> Android SDK),"
    Write-Host "then re-run this script. Or set it by hand:"
    Write-Host ""
    Write-Host '    "sdk.dir=C:\\path\\to\\Sdk" | Set-Content local.properties'
    Write-Host ""
    exit 1
}

# Gradle reads this file as Java properties, where a backslash is an escape character. An
# unescaped Windows path silently resolves to the wrong directory.
$escaped = $sdk -replace '\\', '\\\\'
"sdk.dir=$escaped" | Set-Content -Path (Join-Path $PSScriptRoot "local.properties") -Encoding ASCII

Write-Host ""
Write-Host "SDK found:  $sdk" -ForegroundColor Green
Write-Host "Wrote:      local.properties"
Write-Host ""

if (-not $env:ANDROID_HOME) {
    Write-Host "ANDROID_HOME is not set. Setting it means no future copy of this project needs"
    Write-Host "this script at all."
    $answer = Read-Host "Set ANDROID_HOME for your user account now? (y/N)"
    if ($answer -eq "y") {
        [Environment]::SetEnvironmentVariable("ANDROID_HOME", $sdk, "User")
        Write-Host "Set. Open a new terminal for it to take effect." -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "Now run:  .\gradlew.bat :app:assembleDebug"
Write-Host ""
