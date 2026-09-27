#!/bin/bash

# Sistema de Psicología - Ejecutor
# Este script inicia la aplicación del Sistema de Psicología

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Verificar si existe el JAR
if [ ! -f "$SCRIPT_DIR/dist/SistemaPsicologia.jar" ]; then
    echo "Error: No se encontró SistemaPsicologia.jar"
    echo "Por favor, compila el proyecto primero con: Shift+F11 en NetBeans"
    exit 1
fi

# Verificar si Java está instalado
if ! command -v java &> /dev/null; then
    echo "Error: Java no está instalado"
    echo "Por favor, instala Java 8 o superior"
    exit 1
fi

# Iniciar aplicación
echo "Iniciando Sistema de Psicología..."
java -jar "$SCRIPT_DIR/dist/SistemaPsicologia.jar"

if [ $? -ne 0 ]; then
    echo "Error al ejecutar la aplicación"
    exit 1
fi

exit 0
