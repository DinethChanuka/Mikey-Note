@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0"

echo =============================================
echo   Building Mikey Note (Windows)
echo =============================================

rem Ensure lib and bin directories exist
if not exist "lib" mkdir lib
if not exist "bin" mkdir bin

rem Check SQLite JDBC driver
if not exist "lib\sqlite-jdbc-3.49.1.0.jar" (
    echo [Build] Downloading SQLite JDBC driver...
    curl -L -s -o "lib\sqlite-jdbc-3.49.1.0.jar" "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.49.1.0/sqlite-jdbc-3.49.1.0.jar"
    if not exist "lib\sqlite-jdbc-3.49.1.0.jar" (
        powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.49.1.0/sqlite-jdbc-3.49.1.0.jar' -OutFile 'lib\sqlite-jdbc-3.49.1.0.jar'"
    )
)

echo [Build] Collecting Java sources...
dir /s /b src\main\java\*.java > sources.txt

echo [Build] Compiling Java classes...
javac -cp "lib\sqlite-jdbc-3.49.1.0.jar;lib\javafx-sdk-24\lib\*" -d bin @sources.txt
if exist sources.txt del sources.txt

if exist "src\test\java" (
    dir /s /b src\test\java\*.java > test_sources.txt
    javac -cp "lib\sqlite-jdbc-3.49.1.0.jar;lib\javafx-sdk-24\lib\*;bin" -d bin @test_sources.txt
    if exist test_sources.txt del test_sources.txt
)

echo [Build] Copying application resources...
if exist "src\main\resources" (
    xcopy /E /I /Y "src\main\resources" "bin\" >nul 2>&1
)
if exist "loadwindow" (
    if not exist "bin\loadwindow" mkdir "bin\loadwindow"
    xcopy /E /I /Y "loadwindow" "bin\loadwindow\" >nul 2>&1
)

echo [Build] Packaging wordbook.jar...
if not exist "META-INF" mkdir "META-INF"
(
echo Manifest-Version: 1.0
echo Main-Class: com.wordbook.WordBookApplication
echo Class-Path: lib/sqlite-jdbc-3.49.1.0.jar
) > META-INF\MANIFEST.MF

jar cfm wordbook.jar META-INF\MANIFEST.MF -C bin .
if exist "META-INF" rmdir /s /q "META-INF"

echo [Build] Success! Output: wordbook.jar
echo Run using: run.bat or java -jar wordbook.jar
