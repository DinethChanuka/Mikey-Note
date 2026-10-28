# Mikey Note Windows PowerShell Test Suite Runner
$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  Running WordBook Automated Tests (PowerShell)   " -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

if (-not (Test-Path "bin")) { New-Item -ItemType Directory -Path "bin" | Out-Null }
$Sources = Get-ChildItem -Path "src" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
Set-Content -Path "sources.txt" -Value $Sources

$Cp = "lib/sqlite-jdbc-3.49.1.0.jar;bin"
javac -cp $Cp -d bin @sources.txt
Remove-Item "sources.txt" -Force -ErrorAction SilentlyContinue

java --enable-native-access=ALL-UNNAMED -cp $Cp com.wordbook.WordBookTestSuite
