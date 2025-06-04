#!/bin/bash
# 使用指定IntelliJ版本构建项目的shell脚本
# 使用方法: ./build-with-version.sh [version] [sbt-task1] [sbt-task2] ...
# 例如: ./build-with-version.sh 241 clean compile test
# 支持的版本: 233, 241, 252

if [ $# -eq 0 ]; then
    echo "Usage: $0 [version] [sbt-task1] [sbt-task2] ..."
    echo "Example: $0 241 clean compile test"
    echo "Example: $0 233 \"clean; compile; test\""
    echo "Supported versions: 233, 241, 252"
    exit 1
fi

VERSION=$1
shift  # 移除第一个参数，剩下的都是sbt任务

# 如果没有提供sbt任务，默认使用compile
if [ $# -eq 0 ]; then
    SBT_TASKS="compile"
else
    # 将所有剩余参数组合成sbt任务字符串
    SBT_TASKS="$*"
fi

echo "Building with IntelliJ version $VERSION..."
echo "Running: sbt -Dintellij.version=$VERSION $SBT_TASKS"

sbt -Dintellij.version=$VERSION $SBT_TASKS 