#!/bin/sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
if [ -n "${SWIPE_JAVA_HOME:-}" ]; then
    if [ ! -x "$SWIPE_JAVA_HOME/bin/java" ] && [ ! -x "$SWIPE_JAVA_HOME/bin/java.exe" ]; then
        printf '%s\n' 'SWIPE_JAVA_HOME must point to an existing JDK installation.' >&2
        exit 1
    fi
    export JAVA_HOME="$SWIPE_JAVA_HOME"
fi
if [ "$#" -eq 0 ]; then
    set -- :policy:test :app:lintDebug :app:assembleDebug
fi
exec ./gradlew "$@"
