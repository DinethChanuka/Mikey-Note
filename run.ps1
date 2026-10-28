# Mikey Note Root PowerShell Launcher
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location (Join-Path $ScriptDir "Source")
& ".\run.ps1" $args
Set-Location $ScriptDir
