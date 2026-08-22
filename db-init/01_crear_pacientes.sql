-- Script para crear la tabla de pacientes
CREATE TABLE IF NOT EXISTS pacientes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE,
    telefono VARCHAR(20),
    documento VARCHAR(20) UNIQUE,
    fecha_nacimiento DATE,
    genero VARCHAR(20),
    direccion VARCHAR(255),
    ciudad VARCHAR(100),
    estado_civil VARCHAR(50),
    ocupacion VARCHAR(100),
    antecedentes LONGTEXT,
    observaciones LONGTEXT,
    fecha_registro DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_nombre (nombre),
    INDEX idx_documento (documento),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
