# Nettoie les caches locaux verrouillés puis lance un build debug.
# Exécuter depuis la racine du projet : .\scripts\clean-and-build.ps1

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectRoot

Write-Host "Arrêt des daemons Gradle..."
& .\gradlew.bat --stop 2>$null

$pathsToRemove = @(
    "$ProjectRoot\app\build",
    "$ProjectRoot\build",
    "$ProjectRoot\.gradle\8.9\executionHistory",
    "$env:LOCALAPPDATA\AndroidBuild\Movies_App-android"
)

foreach ($path in $pathsToRemove) {
    if (Test-Path $path) {
        Write-Host "Suppression : $path"
        Remove-Item -LiteralPath $path -Recurse -Force -ErrorAction SilentlyContinue
    }
}

Write-Host "Compilation debug..."
& .\gradlew.bat assembleDebug --no-daemon
exit $LASTEXITCODE
