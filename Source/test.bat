@echo off
setlocal

cd /d "%~dp0"

echo ==================================================
echo   Running WordBook Automated Tests (Windows)
echo ==================================================

if not exist "bin" mkdir bin
dir /s /b src\*.java > sources.txt
javac -cp "lib\sqlite-jdbc-3.49.1.0.jar;bin" -d bin @sources.txt
if exist sources.txt del sources.txt

java --enable-native-access=ALL-UNNAMED -cp "lib\sqlite-jdbc-3.49.1.0.jar;bin" com.wordbook.WordBookTestSuite
