# Mikey Note Windows PowerShell Launch Script
$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

if (-not (Test-Path "bin/com/wordbook/WordBookApplication.class")) {
    Write-Host "[Run] Building project first..." -ForegroundColor Yellow
    & ".\build.ps1"
}

$Cp = "lib/sqlite-jdbc-3.49.1.0.jar;lib/javafx-sdk-24/lib/*;bin"

java `
  -XX:+UseParallelGC `
  --sun-misc-unsafe-memory-access=allow `
  -Dcom.sun.webkit.useHTTP2Loader=false `
  -Dcom.sun.webkit.useJIT=false `
  -Dawt.useSystemAAFontSettings=on `
  -Dswing.aatext=true `
  --enable-native-access=ALL-UNNAMED `
  -cp $Cp `
  com.wordbook.WordBookApplication $args
