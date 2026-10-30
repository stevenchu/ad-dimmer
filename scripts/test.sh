#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if [[ -n "${JAVA_HOME:-}" ]]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
mkdir -p "$ROOT/build/tests"
javac -d "$ROOT/build/tests" "$ROOT/app/src/main/java/org/stevenchu/screencover/Config.java" "$ROOT/tests/ConfigTest.java"
java -cp "$ROOT/build/tests" ConfigTest
