# ========================================
# Generador de EXE Standalone
# Sistema de Psicologia
# ========================================

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$JarFile = Join-Path $ScriptDir "dist\SistemaPsicologia.jar"
$ExeFile = Join-Path $ScriptDir "dist\SistemaPsicologia.exe"
$TempDir = Join-Path $env:TEMP "SistemaPsicologia_Build"

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "  Generador de EXE - Sistema de Psicologia" -ForegroundColor Green
Write-Host "========================================`n" -ForegroundColor Green

# Verificar JAR
if (!(Test-Path $JarFile)) {
    Write-Host "ERROR: No se encontro $JarFile" -ForegroundColor Red
    Write-Host "Asegurate de compilar primero (Shift+F11 en NetBeans)" -ForegroundColor Red
    exit 1
}

Write-Host "JAR encontrado: $JarFile" -ForegroundColor Cyan
Write-Host "Tamano: $((Get-Item $JarFile).Length / 1MB) MB" -ForegroundColor Cyan

# Crear directorio temporal
if (Test-Path $TempDir) {
    Remove-Item $TempDir -Recurse -Force
}
New-Item -ItemType Directory -Path $TempDir -Force | Out-Null

# Crear launcher.bat temporal
$LauncherBat = @"
@echo off
cd /d "%~dp0"
java -jar SistemaPsicologia.jar
"@

$LauncherPath = Join-Path $TempDir "launcher.bat"
Set-Content -Path $LauncherPath -Value $LauncherBat -Encoding ASCII

# Copiar JAR
Copy-Item $JarFile -Destination (Join-Path $TempDir "SistemaPsicologia.jar")

Write-Host "`nArchivos preparados en: $TempDir" -ForegroundColor Cyan

# Crear EXE usando iexpress (herramienta nativa de Windows)
$SedFile = Join-Path $TempDir "package.sed"
$SedContent = @"
[Version]
Class=IEXPRESS
SEDVersion=3

[Options]
PackagePurpose=InstallApp
ShowInstallProgramWindow=1
HideExtractAnimation=1
UseLongFileName=1
InsideCompressed=0
CAB_FixedSize=0
CAB_ResvCodeSigning=0
RebootMode=I
InstallPrompt=%InstallPrompt%
DisplayLicense=%DisplayLicense%
FinishMessage=%FinishMessage%
TargetName=$ExeFile
FriendlyName=Sistema de Psicologia
AppLaunched=launcher.bat
PostInstallCmd=<None>
AdminQuietInstCmd=
UserQuietInstCmd=
SourceFiles=SourceFiles

[Strings]
InstallPrompt=Selecciona una carpeta de destino para Sistema de Psicologia
DisplayLicense=
FinishMessage=Sistema de Psicologia ha sido instalado correctamente.

[SourceFiles]
SourceFiles0=$TempDir
"@

Set-Content -Path $SedFile -Value $SedContent -Encoding ASCII

# Usar iexpress para crear EXE
Write-Host "`nGenerando EXE con iexpress..." -ForegroundColor Yellow
& iexpress /N /Q /M $SedFile

if (Test-Path $ExeFile) {
    Write-Host "`n========================================" -ForegroundColor Green
    Write-Host "  EXITO: EXE generado correctamente" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "`nUbicacion: $ExeFile" -ForegroundColor Cyan
    Write-Host "Tamano: $((Get-Item $ExeFile).Length / 1MB) MB`n" -ForegroundColor Cyan
    
    # Limpiar temporal
    Remove-Item $TempDir -Recurse -Force -ErrorAction SilentlyContinue
    
    Write-Host "Ahora puedes ejecutar: $ExeFile" -ForegroundColor Green
    Write-Host "`nOpciones:" -ForegroundColor Yellow
    Write-Host "1. Doble clic en el EXE" -ForegroundColor White
    Write-Host "2. Desde PowerShell: & '$ExeFile'" -ForegroundColor White
} else {
    Write-Host "`nERROR: No se pudo generar el EXE" -ForegroundColor Red
    exit 1
}
