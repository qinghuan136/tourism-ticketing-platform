#!/usr/bin/env sh
set -eu

docker compose up --build -d
docker compose ps

printf '\nTourist: http://localhost:3000\n'
printf 'Operator: http://localhost:3001\n'
printf 'Swagger: http://localhost:8080/swagger-ui/index.html\n'
