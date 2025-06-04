@echo off
setlocal enabledelayedexpansion

REM 使用指定IntelliJ版本构建项目的批处理脚本
REM 使用方法: build-with-version.bat [version] [sbt-task1] [sbt-task2] ...
REM 例如: build-with-version.bat 241 clean compile test
REM 支持的版本: 233, 241, 252

if "%1"=="" (
    echo Usage: build-with-version.bat [version] [sbt-task1] [sbt-task2] ...
    echo Example: build-with-version.bat 241 clean compile test
    echo Example: build-with-version.bat 233 "clean; compile; test"
    echo Supported versions: 233, 241, 252
    exit /b 1
)

REM 设置版本
set INTELLIJ_VERSION=%1

REM 移除第一个参数，获取所有sbt任务
shift
set SBT_TASKS=
:loop
if "%1"=="" goto done
if defined SBT_TASKS (
    set SBT_TASKS=!SBT_TASKS! %1
) else (
    set SBT_TASKS=%1
)
shift
goto loop
:done

REM 如果没有提供sbt任务，默认使用compile
if not defined SBT_TASKS set SBT_TASKS=compile

echo Building with IntelliJ version !INTELLIJ_VERSION!...
echo Running: sbt -Dintellij.version=!INTELLIJ_VERSION! !SBT_TASKS!

sbt -Dintellij.version=!INTELLIJ_VERSION! !SBT_TASKS! 