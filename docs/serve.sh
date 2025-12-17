#!/bin/bash
# 简单的本地服务器脚本，用于预览构建后的文档

cd "$(dirname "$0")/.vitepress/dist"

echo "正在启动本地服务器..."
echo "访问地址: http://localhost:8080"
echo "按 Ctrl+C 停止服务器"
echo ""

# 尝试使用 Python 3 的 http.server
if command -v python3 &> /dev/null; then
    python3 -m http.server 8080
elif command -v python &> /dev/null; then
    python -m http.server 8080
else
    echo "错误: 未找到 Python，请安装 Python 3"
    exit 1
fi

