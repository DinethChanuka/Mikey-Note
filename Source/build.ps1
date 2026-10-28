# Mikey Note Windows PowerShell Build Script
$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  Building Mikey Note (Windows PowerShell)   " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

if (-not (Test-Path "lib")) { New-Item -ItemType Directory -Path "lib" | Out-Null }
if (-not (Test-Path "bin")) { New-Item -ItemType Directory -Path "bin" | Out-Null }

$SqliteJar = "lib/sqlite-jdbc-3.49.1.0.jar"
if (-not (Test-Path $SqliteJar)) {
    Write-Host "[Build] Downloading SQLite JDBC driver..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.49.1.0/sqlite-jdbc-3.49.1.0.jar" -OutFile $SqliteJar
}

Write-Host "[Build] Compiling Java classes..." -ForegroundColor Green
$Sources = Get-ChildItem -Path "src/main/java" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
Set-Content -Path "sources.txt" -Value $Sources

$Cp = "lib/sqlite-jdbc-3.49.1.0.jar;lib/javafx-sdk-24/lib/*"
javac -cp $Cp -d bin @sources.txt
Remove-Item "sources.txt" -Force -ErrorAction SilentlyContinue

if (Test-Path "src/test/java") {
    $TestSources = Get-ChildItem -Path "src/test/java" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
    Set-Content -Path "test_sources.txt" -Value $TestSources
    javac -cp "$Cp;bin" -d bin @test_sources.txt
    Remove-Item "test_sources.txt" -Force -ErrorAction SilentlyContinue
}

Write-Host "[Build] Copying application resources..." -ForegroundColor Green
if (Test-Path "src/main/resources") {
    Copy-Item -Path "src/main/resources/*" -Destination "bin" -Recurse -Force -ErrorAction SilentlyContinue
}
if (Test-Path "loadwindow") {
    if (-not (Test-Path "bin/loadwindow")) { New-Item -ItemType Directory -Path "bin/loadwindow" | Out-Null }
    Copy-Item -Path "loadwindow/*" -Destination "bin/loadwindow" -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host "[Build] Packaging wordbook.jar..." -ForegroundColor Green
if (-not (Test-Path "META-INF")) { New-Item -ItemType Directory -Path "META-INF" | Out-Null }
@"
Manifest-Version: 1.0
Main-Class: com.wordbook.WordBookApplication
Class-Path: lib/sqlite-jdbc-3.49.1.0.jar
"@ | Set-Content -Path "META-INF/MANIFEST.MF"

jar cfm wordbook.jar META-INF/MANIFEST.MF -C bin .
Remove-Item "META-INF" -Recurse -Force -ErrorAction SilentlyContinue

Write-Host "[Build] Success! Output: wordbook.jar" -ForegroundColor Green
Write-Host "Run using: .\run.ps1 or java -jar wordbook.jar"
