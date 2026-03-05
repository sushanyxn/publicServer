#!/usr/bin/env sh
# 在 WSL2 中运行 slg-framework-test 集成测试（需在项目根目录执行：./slg-framework-test/run-integration-tests-wsl.sh）

set -e
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

# 若 WSL 中未设 JAVA_HOME 且使用 Windows 的 JDK（按需取消注释并修改路径）
# export JAVA_HOME="/mnt/c/Program Files/Java/jdk-21"

echo "项目根目录: $ROOT_DIR"
echo "执行: mvn test -pl slg-framework-test"
mvn test -pl slg-framework-test
