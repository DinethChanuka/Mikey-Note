@echo off
setlocal

cd /d "%~dp0"

if not exist "bin\com\wordbook\WordBookApplication.class" (
    echo [Run] Building project first...
    call build.bat
)

java ^
  -XX:+UseParallelGC ^
  --sun-misc-unsafe-memory-access=allow ^
  -Dcom.sun.webkit.useHTTP2Loader=false ^
  -Dcom.sun.webkit.useJIT=false ^
  -Dawt.useSystemAAFontSettings=on ^
  -Dswing.aatext=true ^
  --enable-native-access=ALL-UNNAMED ^
  -cp "lib\sqlite-jdbc-3.49.1.0.jar;lib\javafx-sdk-24\lib\*;bin" ^
  com.wordbook.WordBookApplication %*
