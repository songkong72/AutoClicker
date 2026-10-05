@echo off
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build-apk.ps1" %1
echo.
echo Done. Details are in build-log.txt
pause
