@echo off
setlocal

cd /d "%~dp0.."

rem 首次运行时创建本地演示配置；已有 .env 不会被覆盖。
if not exist ".env" copy /Y ".env.example" ".env" >nul

docker compose up --build -d
if errorlevel 1 exit /b 1

docker compose ps
echo.
echo Tourist: http://localhost:3000
echo Operator: http://localhost:3001
echo Swagger: http://localhost:8080/swagger-ui/index.html
