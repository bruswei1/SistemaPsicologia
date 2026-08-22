-- Crear tabla usuarios
CREATE TABLE IF NOT EXISTS usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    usuario VARCHAR(50) UNIQUE NOT NULL,
    contrasena VARCHAR(255) NOT NULL,
    rol VARCHAR(50) NOT NULL, -- admin, psicologo, secretaria
    activo BOOLEAN DEFAULT true,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso TIMESTAMP,
    INDEX idx_usuario (usuario),
    INDEX idx_rol (rol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Crear tabla historia_psicologica
CREATE TABLE IF NOT EXISTS historia_psicologica (
    id INT AUTO_INCREMENT PRIMARY KEY,
    paciente_id INT NOT NULL,
    psicologo_id INT,
    antecedentes LONGTEXT,
    motivo_consulta TEXT,
    observaciones_generales LONGTEXT,
    diagnostico TEXT,
    tratamiento TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ultima_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (paciente_id) REFERENCES pacientes(id) ON DELETE CASCADE,
    FOREIGN KEY (psicologo_id) REFERENCES usuarios(id),
    INDEX idx_paciente (paciente_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Crear tabla sesiones
CREATE TABLE IF NOT EXISTS sesiones (
    id INT AUTO_INCREMENT PRIMARY KEY,
    paciente_id INT NOT NULL,
    psicologo_id INT NOT NULL,
    fecha_sesion DATE NOT NULL,
    hora_inicio VARCHAR(5),
    hora_fin VARCHAR(5),
    notas_sesion LONGTEXT,
    observaciones TEXT,
    estado VARCHAR(50) DEFAULT 'programada', -- programada, completada, cancelada
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (paciente_id) REFERENCES pacientes(id) ON DELETE CASCADE,
    FOREIGN KEY (psicologo_id) REFERENCES usuarios(id),
    INDEX idx_fecha (fecha_sesion),
    INDEX idx_psicologo (psicologo_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Crear tabla turnos
CREATE TABLE IF NOT EXISTS turnos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    paciente_id INT NOT NULL,
    psicologo_id INT NOT NULL,
    fecha DATE NOT NULL,
    hora VARCHAR(5) NOT NULL,
    estado VARCHAR(50) DEFAULT 'confirmado', -- confirmado, cancelado, completado
    notas TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (paciente_id) REFERENCES pacientes(id) ON DELETE CASCADE,
    FOREIGN KEY (psicologo_id) REFERENCES usuarios(id),
    INDEX idx_fecha (fecha),
    INDEX idx_psicologo (psicologo_id),
    UNIQUE KEY unique_turno (paciente_id, psicologo_id, fecha, hora)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insertar usuario admin por defecto
INSERT IGNORE INTO usuarios (nombre, email, usuario, contrasena, rol, activo) 
VALUES ('Administrador', 'admin@psicologia.com', 'admin', 'admin123', 'admin', true);

INSERT IGNORE INTO usuarios (nombre, email, usuario, contrasena, rol, activo) 
VALUES ('Dr. Psicólogo', 'psicologo@psicologia.com', 'psicologo', 'psico123', 'psicologo', true);

INSERT IGNORE INTO usuarios (nombre, email, usuario, contrasena, rol, activo) 
VALUES ('Secretaria', 'secretaria@psicologia.com', 'secretaria', 'secre123', 'secretaria', true);
