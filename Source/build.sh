#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

echo "============================================="
echo "  Building Mikey Note (Java 24 + SQLite)     "
echo "============================================="

# Ensure lib directory and sqlite-jdbc driver exist
mkdir -p lib bin
if [ ! -f "lib/sqlite-jdbc-3.49.1.0.jar" ]; then
    echo "[Build] Downloading SQLite JDBC driver..."
    curl -L -s -o lib/sqlite-jdbc-3.49.1.0.jar https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.49.1.0/sqlite-jdbc-3.49.1.0.jar
fi

echo "[Build] Compiling Java classes..."
javac -cp "lib/sqlite-jdbc-3.49.1.0.jar:lib/javafx-sdk-24/lib/*" -d bin $(find src/main/java -name "*.java")
if [ -d "src/test/java" ]; then
    javac -cp "lib/sqlite-jdbc-3.49.1.0.jar:lib/javafx-sdk-24/lib/*:bin" -d bin $(find src/test/java -name "*.java")
fi

echo "[Build] Copying application resources..."
if [ -d "src/main/resources" ]; then
    cp -r src/main/resources/* bin/ 2>/dev/null || true
fi
if [ -d "loadwindow" ]; then
    mkdir -p bin/loadwindow
    cp -r loadwindow/* bin/loadwindow/ 2>/dev/null || true
fi

echo "[Build] Packaging wordbook.jar..."
mkdir -p META-INF
cat << 'EOF' > META-INF/MANIFEST.MF
Manifest-Version: 1.0
Main-Class: com.wordbook.WordBookApplication
Class-Path: lib/sqlite-jdbc-3.49.1.0.jar
EOF

jar cfm wordbook.jar META-INF/MANIFEST.MF -C bin .
rm -rf META-INF

echo "[Build] Success! Output: wordbook.jar"
echo "Run using: ./run.sh or java -jar wordbook.jar"
