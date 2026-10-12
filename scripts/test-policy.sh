#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -n "${SWIPE_JAVA_HOME:-${JAVA_HOME:-}}" ]]; then
    jdk="${SWIPE_JAVA_HOME:-$JAVA_HOME}"
    java="$jdk/bin/java"
    javac="$jdk/bin/javac"
else
    java="$(command -v java)"
    javac="$(command -v javac)"
fi
version="$("$java" -version 2>&1)"
if [[ "$version" != *'version "17.'* ]]; then
    printf 'JDK 17 required; found: %s\n' "$version" >&2
    exit 1
fi
output="policy/build/offline-tests"
mkdir -p "$output"
sources=()
while IFS= read -r -d '' source; do sources+=("$source"); done < <(find policy/src/main/java -name '*.java' -print0)
sources+=(policy/src/test/java/com/local/swipepilot/PolicyTest.java policy/src/test/java/com/local/swipepilot/AdWaitRepro.java)
"$javac" -encoding UTF-8 --release 8 -d "$output" "${sources[@]}"
"$java" -Dfile.encoding=UTF-8 -cp "$output" com.local.swipepilot.PolicyTest
if [[ "${1:-}" == '--repro' ]]; then
    "$java" -Dfile.encoding=UTF-8 -cp "$output" com.local.swipepilot.AdWaitRepro
fi
