SET NAMES utf8mb4;

CREATE TABLE usuarios (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_usuarios_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE equipos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(120) NOT NULL,
    categoria VARCHAR(80) NOT NULL,
    creador_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_equipos_creador FOREIGN KEY (creador_id)
        REFERENCES usuarios (id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE miembros_equipo (
    equipo_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    rol ENUM('ENTRENADOR', 'JUGADOR') NOT NULL,
    dorsal TINYINT UNSIGNED NULL,
    PRIMARY KEY (equipo_id, usuario_id),
    CONSTRAINT uq_miembros_equipo_dorsal UNIQUE (equipo_id, dorsal),
    CONSTRAINT fk_miembros_equipo_equipo FOREIGN KEY (equipo_id)
        REFERENCES equipos (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_miembros_equipo_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT ck_miembros_equipo_dorsal CHECK (dorsal IS NULL OR dorsal BETWEEN 0 AND 99)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE eventos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    equipo_id BIGINT NOT NULL,
    tipo ENUM('ENTRENAMIENTO', 'PARTIDO') NOT NULL,
    titulo VARCHAR(160) NOT NULL,
    fecha_hora DATETIME NOT NULL,
    lugar VARCHAR(160) NOT NULL,
    rival VARCHAR(120) NULL,
    PRIMARY KEY (id),
    KEY ix_eventos_equipo_fecha (equipo_id, fecha_hora),
    CONSTRAINT fk_eventos_equipo FOREIGN KEY (equipo_id)
        REFERENCES equipos (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE respuestas (
    evento_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    estado ENUM('VOY', 'NO_VOY', 'QUIZAS') NOT NULL,
    comentario VARCHAR(500) NULL,
    fecha_respuesta DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (evento_id, usuario_id),
    CONSTRAINT fk_respuestas_evento FOREIGN KEY (evento_id)
        REFERENCES eventos (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_respuestas_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO usuarios (id, nombre, email) VALUES
    (1, 'Carlos Ruiz', 'carlos.ruiz@example.com'),
    (2, 'Lucas Pérez', 'lucas.perez@example.com'),
    (3, 'Marcos Díaz', 'marcos.diaz@example.com'),
    (4, 'Hugo Sanz', 'hugo.sanz@example.com'),
    (5, 'Daniel Vega', 'daniel.vega@example.com'),
    (6, 'Álex Moreno', 'alex.moreno@example.com'),
    (7, 'Pablo Gil', 'pablo.gil@example.com'),
    (8, 'Sofía Gil', 'sofia.gil@example.com');

INSERT INTO equipos (id, nombre, categoria, creador_id) VALUES
    (1, 'Minibasket 2016', 'Minibasket', 1),
    (2, 'Minibasket 2018', 'Minibasket', 1),
    (3, 'Cadete Femenino', 'Cadete', 8);

INSERT INTO miembros_equipo (equipo_id, usuario_id, rol, dorsal) VALUES
    (1, 1, 'ENTRENADOR', NULL),
    (1, 2, 'JUGADOR', 10),
    (1, 3, 'JUGADOR', 11),
    (1, 4, 'JUGADOR', 12),
    (1, 5, 'JUGADOR', 13),
    (1, 6, 'JUGADOR', 14),
    (1, 7, 'JUGADOR', 15),
    (2, 1, 'ENTRENADOR', NULL),
    (3, 8, 'ENTRENADOR', NULL),
    (3, 1, 'JUGADOR', 7);

INSERT INTO eventos (id, equipo_id, tipo, titulo, fecha_hora, lugar, rival) VALUES
    (1, 1, 'ENTRENAMIENTO', 'Entrenamiento semanal', DATE_ADD(NOW(), INTERVAL 1 DAY), 'Pabellón Municipal', NULL),
    (2, 1, 'ENTRENAMIENTO', 'Entrenamiento de tiro', DATE_ADD(NOW(), INTERVAL 3 DAY), 'Pabellón Municipal', NULL),
    (3, 1, 'PARTIDO', 'Jornada de liga', DATE_ADD(NOW(), INTERVAL 5 DAY), 'Pabellón Municipal', 'CB Pozuelo'),
    (4, 2, 'ENTRENAMIENTO', 'Entrenamiento de equipo', DATE_ADD(NOW(), INTERVAL 2 DAY), 'Polideportivo Norte', NULL),
    (5, 3, 'ENTRENAMIENTO', 'Entrenamiento cadete', DATE_ADD(NOW(), INTERVAL 4 DAY), 'Pabellón Sur', NULL),
    (6, 1, 'PARTIDO', 'Jornada anterior', DATE_SUB(NOW(), INTERVAL 7 DAY), 'Pabellón Municipal', 'CB Alcobendas'),
    (7, 3, 'PARTIDO', 'Partido de preparación', DATE_SUB(NOW(), INTERVAL 2 DAY), 'Pabellón Sur', 'CB Leganés');

INSERT INTO respuestas (evento_id, usuario_id, estado, comentario) VALUES
    (1, 2, 'VOY', 'Allí estaré'),
    (1, 3, 'NO_VOY', 'Tengo otra actividad'),
    (1, 4, 'QUIZAS', 'Lo confirmaré mañana'),
    (2, 5, 'VOY', NULL),
    (2, 6, 'QUIZAS', NULL),
    (3, 2, 'VOY', NULL),
    (3, 7, 'NO_VOY', 'Estoy fuera'),
    (4, 1, 'VOY', NULL),
    (5, 1, 'QUIZAS', 'Pendiente de confirmar'),
    (6, 2, 'VOY', NULL),
    (6, 3, 'NO_VOY', NULL),
    (7, 1, 'VOY', NULL);