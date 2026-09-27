========================================
SISTEMA DE PSICOLOGÍA
========================================

## 1. REQUISITOS

- Java 8 o superior
- MySQL 8.0 o superior
- Docker (opcional, para ejecutar MySQL en contenedor)

## 2. INSTALACIÓN

### Paso 1: Instalar Java
Descarga e instala Java desde: https://www.oracle.com/java/technologies/downloads/

### Paso 2: Instalar MySQL
Opción A - Con Docker (recomendado):
```bash
docker run -d --name psicologia_mysql -p 3306:3306 -e MYSQL_DATABASE=psicologia -e MYSQL_ALLOW_EMPTY_PASSWORD=yes mysql:8.0
```

Opción B - Instalación local:
Descarga MySQL desde: https://dev.mysql.com/downloads/mysql/

### Paso 3: Compilar el proyecto
En NetBeans:
1. Abre el proyecto: File → Open Project
2. Presiona: Shift + F11 (Clean and Build)
3. Espera a que compile exitosamente

## 3. EJECUTAR LA APLICACIÓN

### Windows:
Haz doble clic en: `iniciar.bat`

O desde PowerShell:
```powershell
cd C:\ruta\del\proyecto
java -jar dist\SistemaPsicologia.jar
```

### Linux/Mac:
```bash
cd /ruta/del/proyecto
chmod +x iniciar.sh
./iniciar.sh
```

O directamente:
```bash
java -jar dist/SistemaPsicologia.jar
```

## 4. CREDENCIALES DE PRUEBA

Usuario: **admin**
Contraseña: **admin123**
Rol: Administrador

Usuario: **psicologo**
Contraseña: **psico123**
Rol: Psicólogo

Usuario: **secretaria**
Contraseña: **secre123**
Rol: Secretaria

## 5. ESTRUCTURA DE CARPETAS

```
SistemaPsicologia/
├── src/                      # Código fuente Java
│   ├── conexion/            # Gestión de conexión
│   ├── dao/                 # Data Access Objects
│   ├── modelos/             # Entidades
│   ├── util/                # Utilidades
│   └── Vista/               # Interfaces gráficas
├── dist/                    # JAR ejecutable (generado)
├── build/                   # Compilación (generado)
├── db-init/                 # Scripts SQL de inicialización
├── iniciar.bat              # Script para Windows
├── iniciar.sh               # Script para Linux/Mac
├── ARQUITECTURA_OFICIAL.txt # Documentación técnica
└── README.md               # Este archivo
```

## 6. FUNCIONALIDADES

✅ Autenticación con roles (admin, psicólogo, secretaria)
✅ Gestión de pacientes (registrar, buscar, modificar, eliminar)
✅ Gestión de turnos (crear, ver, cancelar)
✅ Historia psicológica (antecedentes, diagnóstico, tratamiento)
✅ Sesiones clínicas (registrar notas)
✅ Dashboard (estadísticas en tiempo real)
✅ Búsqueda avanzada de pacientes con detalles completos
✅ Exportación de reportes

## 7. BASE DE DATOS

La aplicación crea automáticamente las siguientes tablas:
- usuarios (gestión de usuarios)
- pacientes (registro de pacientes)
- historia_psicologica (historial clínico)
- sesiones (registro de sesiones)
- turnos (agenda de turnos)
- documentos_paciente (archivos adjuntos)
- auditoria (registro de cambios)

## 8. TROUBLESHOOTING

### Error: "No se encuentra Java"
Solución: Instala Java y agrega su ruta al PATH del sistema

### Error: "No se puede conectar a MySQL"
Solución: Verifica que MySQL esté ejecutándose:
```bash
docker ps  # Si usas Docker
mysql -u root -p  # Si está instalado localmente
```

### Error: "Puerto 3306 ya está en uso"
Solución: Cambia el puerto en docker-compose.yml o usa otro puerto

### Error: "No se encontró SistemaPsicologia.jar"
Solución: Compila el proyecto con Shift+F11 en NetBeans

## 9. SOPORTE Y DOCUMENTACIÓN

Para información técnica: Ver ARQUITECTURA_OFICIAL.txt
Para reportes de bugs: Contacta al equipo de desarrollo

========================================
Versión: 1.0
Última actualización: [FECHA]
========================================
