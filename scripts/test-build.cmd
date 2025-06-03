@echo off
setlocal enabledelayedexpansion

REM Test build script to simulate CI process locally on Windows
echo 🔧 Testing local build process...

echo 📦 Installing webview dependencies...
cd webview
call npm ci
if !errorlevel! neq 0 (
    echo ❌ npm ci failed!
    exit /b 1
)
cd ..

echo 🏗️ Building project with SBT...
call sbt clean compile packageArtifactZip
if !errorlevel! neq 0 (
    echo ❌ SBT build failed!
    exit /b 1
)

echo ✅ Build completed successfully!

REM Check if the artifact was created
if exist "target\CodeEpiphany-*.zip" (
    echo 📄 Plugin artifact created:
    dir target\CodeEpiphany-*.zip
) else (
    echo ❌ Plugin artifact not found!
    exit /b 1
)

echo 🎉 All tests passed!
pause 