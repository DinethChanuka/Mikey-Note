# Mikey Note Root PowerShell Build Wrapper
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location (Join-Path $ScriptDir "Source")
& ".\build.ps1"
Set-Location $ScriptDir
