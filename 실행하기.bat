@echo off
cd /d "%~dp0"
python autoclicker.py
if errorlevel 1 (
    echo.
    echo An error occurred while running AutoClicker.
    pause
)
