@echo off
setlocal
cd /d "%~dp0"
set "TARGET_EXE=HVH-Targeting-Win32.exe"
if /I "%PROCESSOR_ARCHITECTURE%"=="AMD64" set "TARGET_EXE=HVH-Targeting-Win64.exe"
if defined PROCESSOR_ARCHITEW6432 set "TARGET_EXE=HVH-Targeting-Win64.exe"
"%TARGET_EXE%" --check-config "%~dp0hvh_targeting.ini"
echo.
echo Press any key to close this window.
pause >nul
