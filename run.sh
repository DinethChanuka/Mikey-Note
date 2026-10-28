#!/usr/bin/env bash
set -e
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR/Source"
chmod +x run.sh build.sh test.sh 2>/dev/null || true
exec ./run.sh "$@"
