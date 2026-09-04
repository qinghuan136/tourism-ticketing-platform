@echo off
setlocal

docker compose up --build -d
if errorlevel 1 exit /b 1

docker compose ps
echo.
echo Tourist: http://localhost:3000
echo Operator: http://localhost:3001
echo Swagger: http://localhost:8080/swagger-ui/index.html
