@echo off
chcp 65001 >nul
cd /d "%~dp0"
REM 通过子批处理启动，避免引号导致窗口闪退；不执行 npm install

echo.
echo 正在一键启动：用户端 + 管理端 + 后端...
echo.

start "用户端 (client)" "%~dp0run-client.bat"
timeout /t 1 /nobreak >nul
start "管理端 (admin)" "%~dp0run-admin.bat"
timeout /t 1 /nobreak >nul
start "后端 (backend)" "%~dp0run-backend.bat"

echo 已启动 3 个窗口：用户端、管理端、后端
echo.
pause
