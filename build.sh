#!/bin/bash
# Build script for cmdai — run this on your machine with JDK 11+
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$SCRIPT_DIR/src/main/java"
BUILD_DIR="$SCRIPT_DIR/build"
JAR_FILE="$SCRIPT_DIR/cmdai.jar"

echo "==> Compiling..."
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/classes"
javac -d "$BUILD_DIR/classes" "$SRC_DIR"/com/cmdai/*.java

echo "==> Packaging $JAR_FILE..."
echo "Main-Class: com.cmdai.CmdAi" > "$BUILD_DIR/MANIFEST.MF"
jar cfm "$JAR_FILE" "$BUILD_DIR/MANIFEST.MF" -C "$BUILD_DIR/classes" .

echo "==> Done! Run with:"
echo "    java -jar cmdai.jar --help"
echo ""
echo "    Or install globally:"
echo "    sudo cp cmdai.jar /usr/local/lib/"
echo "    echo 'alias cmdai=\"java -jar /usr/local/lib/cmdai.jar\"' >> ~/.bashrc"
