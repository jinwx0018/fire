@echo off
setlocal EnableExtensions
cd /d "%~dp0admin"
if not exist "node_modules" (
  echo [admin] npm install ...
  call npm install
  if errorlevel 1 (
    echo [admin] npm install failed.
    pause
    exit /b 1
  )
)
echo [admin] http://localhost:5173
title fire-admin
call npm run dev
pause
