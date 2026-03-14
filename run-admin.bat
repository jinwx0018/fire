@echo off
chcp 65001 >nul
cd /d "%~dp0admin"
title 管理端 (admin)
npm run dev
pause
