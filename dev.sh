#!/bin/bash

# 一键启动开发环境：前后端服务 & FRP 内网穿透
# 用法:
#   ./dev.sh         仅启动前后端本地服务
#   ./dev.sh --frp   启动前后端服务，并开启 FRP 外网穿透

cleanup() {
  echo ""
  echo "正在停止服务..."
  kill $BACKEND_PID $FRONTEND_PID $FRPC_PID 2>/dev/null
  wait $BACKEND_PID $FRONTEND_PID $FRPC_PID 2>/dev/null
  echo "已停止"
  exit 0
}

trap cleanup SIGINT SIGTERM

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

# FRP 穿透（可选）
if [[ "$1" == "--frp" ]]; then
  if ! command -v frpc &>/dev/null; then
    echo "错误: frpc 未安装，请先安装 FRP 客户端"
    echo "  brew install frpc  (macOS)"
    echo "  或从 https://github.com/fatedier/frp/releases 下载"
    exit 1
  fi
  echo "=== 启动 FRP 穿透 ==="
  frpc -c "$PROJECT_DIR/scripts/frpc.toml" &
  FRPC_PID=$!
  echo "FRP: test.wizone.work -> localhost:8080"
fi

echo "=== 启动后端 ==="
cd "$PROJECT_DIR/backend"
JAVA_HOME=/usr/local/opt/openjdk@17 mvn spring-boot:run &
BACKEND_PID=$!

echo "=== 启动前端 ==="
cd "$PROJECT_DIR/frontend/web-admin"
pnpm dev &
FRONTEND_PID=$!

echo ""
echo "后端: http://localhost:8080"
echo "前端: http://localhost:8848"
[[ -n "$FRPC_PID" ]] && echo "FRP: http://test.wizone.work (企微回调: http://test.wizone.work:7001/api/wx/callback)"
echo "按 Ctrl+C 停止所有服务"
echo ""

wait
