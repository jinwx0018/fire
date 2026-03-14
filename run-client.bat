@echo off
chcp 65001 >nul
cd /d "%~dp0client"
title 用户端 (client)
npm run dev
pause
