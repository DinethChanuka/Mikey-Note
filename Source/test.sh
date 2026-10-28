#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

# Rebuild if jar is missing or source files are newer
if [ ! -f "wordbook.jar" ] || [ $(find src -name "*.java" -newer wordbook.jar 2>/dev/null | wc -l) -gt 0 ]; then
    ./build.sh
fi

exec java \
  --sun-misc-unsafe-memory-access=allow \
  --enable-native-access=ALL-UNNAMED \
  -cp "wordbook.jar:lib/*:lib/javafx-sdk-24/lib/*" \
  com.wordbook.WordBookTestSuite "$@"
