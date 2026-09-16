#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$SCRIPT_DIR/.."

# 首次运行时创建本地演示配置；已有 .env 不会被覆盖。
if [ ! -f .env ]; then
  cp .env.example .env
fi

docker compose up --build -d
docker compose ps

printf '\nTourist: http://localhost:3000\n'
printf 'Operator: http://localhost:3001\n'
printf 'Swagger: http://localhost:8080/swagger-ui/index.html\n'
