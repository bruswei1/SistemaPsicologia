@echo off
REM ========================================
REM Generador de EXE - Sistema de Psicologia
REM ========================================

setlocal enabledelayedexpansion

cls
color 0A

echo.
echo ========================================
echo  Generador de EXE
echo  Sistema de Psicologia
echo ========================================
echo.

REM Obtener ruta del script
set SCRIPT_DIR=%~dp0
set PS_SCRIPT=%SCRIPT_DIR%generar-exe.ps1

REM Ejecutar PowerShell
powershell -NoProfile -ExecutionPolicy Bypass -File "%PS_SCRIPT%"

if errorlevel 1 (
    color 0C
    echo.
    echo ERROR: No se pudo generar el EXE
    echo.
) else (
    color 0A
    echo.
    echo EXE generado correctamente
    echo.
)

pause
