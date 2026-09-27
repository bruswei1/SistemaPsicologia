@echo off
REM ========================================
REM SISTEMA DE PSICOLOGÍA - Launcher
REM ========================================
REM Este script inicia la aplicación
REM Requiere: Java 8 o superior instalado
REM Requiere: MySQL 8.0 ejecutándose

color 0A
cls

echo.
echo ========================================
echo    SISTEMA DE PSICOLOGIA
echo ========================================
echo.

REM Obtener ruta del script
set SCRIPT_DIR=%~dp0
set JAR_PATH=%SCRIPT_DIR%dist\SistemaPsicologia.jar

REM Verificar si existe el JAR
if not exist "%JAR_PATH%" (
    color 0C
    echo ERROR: No se encontro SistemaPsicologia.jar
    echo Ubicacion esperada: %JAR_PATH%
    echo.
    pause
    exit /b 1
)

REM Verificar si Java está instalado
java -version >nul 2>&1
if errorlevel 1 (
    color 0C
    echo ERROR: Java no esta instalado
    echo Por favor instala Java 8 o superior desde: https://www.oracle.com/java/
    echo.
    pause
    exit /b 1
)

REM Verificar MySQL
echo Verificando conexion a MySQL...
timeout /t 2 /nobreak >nul

REM Iniciar aplicación
cls
echo Iniciando Sistema de Psicologia...
echo Asegúrate de que MySQL esté ejecutándose en localhost:3306
echo.

java -jar "%JAR_PATH%"

if errorlevel 1 (
    color 0C
    echo.
    echo ERROR: La aplicacion se cerro inesperadamente
    echo.
    pause
    exit /b 1
)

exit /b 0
