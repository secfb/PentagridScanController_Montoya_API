#!/bin/bash
# PentagridScanController Build Script
# This script configures the required Java 21 environment and compiles the project.

set -e

# Define Java Home to Temurin OpenJDK 21 to avoid incompatibility issues with Kotlin compile daemon on newer JDKs
export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

echo "========================================="
echo "[*] Compiling PentagridScanController..."
echo "[*] JAVA_HOME: $JAVA_HOME"
echo "[*] Java version:"
java -version
echo "========================================="

# Execute Gradle jar build
/opt/homebrew/opt/gradle/libexec/bin/gradle clean jar

echo "========================================="
echo "[+] Build completed successfully!"
echo "[+] Output Jar: $(pwd)/build/libs/PentagridScanController-0.2.jar"
echo "========================================="
