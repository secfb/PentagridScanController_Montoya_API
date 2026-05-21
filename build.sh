#!/bin/bash
# PentagridScanController Build Script
# This script intelligently detects the required Java environment and compiles the project.

set -e

# 1. Detect Java 21+
echo "[*] Detecting Java environment..."

# Check if JAVA_HOME is already defined and points to a Java version starting with 21
if [ -n "$JAVA_HOME" ] && "$JAVA_HOME/bin/java" -version 2>&1 | grep -q 'version "21'; then
    echo "[+] Using existing JAVA_HOME: $JAVA_HOME"
elif [ -x "/usr/libexec/java_home" ]; then
    # On macOS, use the official java_home utility to locate Java 21
    if /usr/libexec/java_home -v 21 >/dev/null 2>&1; then
        export JAVA_HOME=$(/usr/libexec/java_home -v 21)
        export PATH="$JAVA_HOME/bin:$PATH"
        echo "[+] Detected Java 21 at: $JAVA_HOME"
    else
        # Fallback to default java_home
        export JAVA_HOME=$(/usr/libexec/java_home)
        export PATH="$JAVA_HOME/bin:$PATH"
        echo "[!] Java 21 not found via java_home. Falling back to default Java: $JAVA_HOME"
    fi
else
    # Fallback to system PATH's java
    if command -v java >/dev/null 2>&1; then
        echo "[!] /usr/libexec/java_home not available. Using system default 'java' in PATH."
    else
        echo "[x] Error: Java is not installed or not in PATH."
        exit 1
    fi
fi

# 2. Detect Gradle
if command -v gradle >/dev/null 2>&1; then
    GRADLE_CMD="gradle"
elif [ -x "/opt/homebrew/opt/gradle/libexec/bin/gradle" ]; then
    GRADLE_CMD="/opt/homebrew/opt/gradle/libexec/bin/gradle"
else
    echo "[x] Error: gradle is not found. Please install Gradle first."
    exit 1
fi

echo "========================================="
echo "[*] Compiling PentagridScanController..."
echo "[*] JAVA_HOME: ${JAVA_HOME:-System Default}"
echo "[*] Gradle: $GRADLE_CMD"
echo "[*] Java version:"
java -version
echo "========================================="

# Execute Gradle jar build
$GRADLE_CMD clean jar

echo "========================================="
echo "[+] Build completed successfully!"
echo "[+] Output Jar: $(pwd)/build/libs/PentagridScanController-0.2.jar"
echo "========================================="
