# Mikey Note Root PowerShell Test Wrapper
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location (Join-Path $ScriptDir "Source")
& ".\test.ps1"
Set-Location $ScriptDir
