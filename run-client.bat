@echo off
setlocal EnableExtensions
cd /d "%~dp0client"
if not exist "node_modules" (
  echo [client] npm install ...
  call npm install
  if errorlevel 1 (
    echo [client] npm install failed.
    pause
    exit /b 1
  )
)
echo [client] http://localhost:5177
title fire-client
call npm run dev
pause
