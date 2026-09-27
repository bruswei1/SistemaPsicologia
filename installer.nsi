; Installer para Sistema de Psicologia
; Requiere: NSIS instalado

!include "MUI2.nsh"
!include "x64.nsh"

; Nombre de la aplicacion
!define APP_NAME "Sistema de Psicologia"
!define APP_VERSION "1.0"
!define APP_PUBLISHER "Desarrollo de Sistemas"
!define APP_EXECUTABLE "SistemaPsicologia.exe"
!define INSTALL_DIR "$PROGRAMFILES\${APP_NAME}"

; Interfaz
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_LANGUAGE "Spanish"

; Informacion del instalador
Name "${APP_NAME} ${APP_VERSION}"
OutFile "dist\SistemaPsicologia-Installer.exe"
InstallDir "${INSTALL_DIR}"
ShowInstDetails show
ShowUninstDetails show

; Instalar
Section "Instalar"
  SetOutPath "$INSTDIR"
  
  ; Copiar archivos
  File "dist\SistemaPsicologia.jar"
  File "dist\SistemaPsicologia.exe"
  File "docker-compose.yml"
  File "README.md"
  File "iniciar.bat"
  
  ; Crear acceso directo en Desktop
  CreateDirectory "$SMPROGRAMS\${APP_NAME}"
  CreateShortCut "$SMPROGRAMS\${APP_NAME}\${APP_NAME}.lnk" "$INSTDIR\${APP_EXECUTABLE}"
  CreateShortCut "$DESKTOP\${APP_NAME}.lnk" "$INSTDIR\${APP_EXECUTABLE}"
  
  ; Crear entrada en Programs and Features
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_NAME}" "DisplayName" "${APP_NAME} ${APP_VERSION}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_NAME}" "UninstallString" "$INSTDIR\uninstall.exe"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_NAME}" "DisplayVersion" "${APP_VERSION}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_NAME}" "Publisher" "${APP_PUBLISHER}"
  
  ; Crear desinstalador
  WriteUninstaller "$INSTDIR\uninstall.exe"
SectionEnd

; Desinstalar
Section "Uninstall"
  RMDir /r "$INSTDIR"
  RMDir /r "$SMPROGRAMS\${APP_NAME}"
  Delete "$DESKTOP\${APP_NAME}.lnk"
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_NAME}"
SectionEnd
