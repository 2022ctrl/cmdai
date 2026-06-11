@echo off
REM Build script for cmdai — run this on Windows with JDK 11+

set SCRIPT_DIR=%~dp0
set SRC_DIR=%SCRIPT_DIR%src\main\java
set BUILD_DIR=%SCRIPT_DIR%build
set JAR_FILE=%SCRIPT_DIR%cmdai.jar

echo ==^> Compiling...
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
mkdir "%BUILD_DIR%\classes"
javac -d "%BUILD_DIR%\classes" "%SRC_DIR%\com\cmdai\*.java"
if %errorlevel% neq 0 (
    echo Build failed!
    exit /b 1
)

echo ==^> Packaging %JAR_FILE%...
echo Main-Class: com.cmdai.CmdAi > "%BUILD_DIR%\MANIFEST.MF"
jar cfm "%JAR_FILE%" "%BUILD_DIR%\MANIFEST.MF" -C "%BUILD_DIR%\classes" .

echo ==^> Done! Run with:
echo     java -jar cmdai.jar --help
