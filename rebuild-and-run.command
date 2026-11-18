#!/bin/bash
# Local macOS helper: build current source, then open the isolated test client.
cd -- "$(dirname -- "$0")" || exit 1

trueaura_java_home="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
if [ -z "$trueaura_java_home" ]; then
    for candidate in "$HOME"/.gradle/jdks/*17*/jdk-17*/Contents/Home; do
        if [ -x "$candidate/bin/javac" ]; then
            trueaura_java_home="$candidate"
            break
        fi
    done
fi
if [ -z "$trueaura_java_home" ]; then
    echo 'JDK 17 not found. Install JDK 17, then run this file again.'
    read -r -p 'Press Enter to close.'
    exit 1
fi

export JAVA_HOME="$trueaura_java_home"
./gradlew test build runClient --console=plain -Dhttps.protocols=TLSv1.2
trueaura_result=$?
if [ "$trueaura_result" -ne 0 ]; then
    echo "Build or game exited with code $trueaura_result. Copy the error back to the chat."
    read -r -p 'Press Enter to close.'
fi
exit "$trueaura_result"
