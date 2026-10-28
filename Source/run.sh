#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

# Build if not already built
if [ ! -f "bin/com/wordbook/WordBookApplication.class" ]; then
    echo "[Run] Building project first..."
    ./build.sh
fi

# Run WordBook application with JavaFX modules enabled and stable flags
exec java \
  -XX:+UseParallelGC \
  --sun-misc-unsafe-memory-access=allow \
  -Dcom.sun.webkit.useHTTP2Loader=false \
  -Dcom.sun.webkit.useJIT=false \
  --module-path "lib/javafx-sdk-24/lib" \
  --add-modules javafx.controls,javafx.web,javafx.swing \
  --enable-native-access=ALL-UNNAMED,javafx.graphics,javafx.web \
  -cp "lib/sqlite-jdbc-3.49.1.0.jar:lib/javafx-sdk-24/lib/*:bin" \
  com.wordbook.WordBookApplication "$@"
