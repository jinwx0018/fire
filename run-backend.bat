@echo off
chcp 65001 >nul
cd /d "%~dp0backend"
title 后端 (backend)
mvn spring-boot:run
pause
