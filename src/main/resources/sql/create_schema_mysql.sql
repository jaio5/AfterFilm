-- Script MySQL revisado (no destructivo) para AfterFilm
-- Asegura compatibilidad con versiones que no soportan CREATE INDEX IF NOT EXISTS
CREATE DATABASE IF NOT EXISTS `AfterFilm` CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
USE `AfterFilm`;

-- Tabla de usuarios
CREATE TABLE IF NOT EXISTS `usuario` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(100) NOT NULL UNIQUE,
  `email` VARCHAR(255),
  `password` VARCHAR(255) NOT NULL,
  `nombre` VARCHAR(255),
  `nivel_critico` DECIMAL(5,2) NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `enabled` TINYINT(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Roles
CREATE TABLE IF NOT EXISTS `role` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL UNIQUE,
  `description` TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Usuario -> roles
CREATE TABLE IF NOT EXISTS `usuario_roles` (
  `usuario_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  PRIMARY KEY (`usuario_id`,`role_id`),
  INDEX (`role_id`),
  CONSTRAINT `fk_usuario_roles_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuario`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_usuario_roles_role` FOREIGN KEY (`role_id`) REFERENCES `role`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Etiquetas
CREATE TABLE IF NOT EXISTS `etiqueta` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL UNIQUE,
  `description` TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `usuario_etiquetas` (
  `usuario_id` BIGINT NOT NULL,
  `etiqueta_id` BIGINT NOT NULL,
  PRIMARY KEY (`usuario_id`,`etiqueta_id`),
  CONSTRAINT `fk_usuario_etiquetas_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuario`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_usuario_etiquetas_etiqueta` FOREIGN KEY (`etiqueta_id`) REFERENCES `etiqueta`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Pelicula
CREATE TABLE IF NOT EXISTS `pelicula` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `tmdb_id` BIGINT UNIQUE,
  `title` VARCHAR(255) NOT NULL,
  `overview` TEXT,
  `release_date` DATE,
  `runtime` INT,
  `poster_url` TEXT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Actor
CREATE TABLE IF NOT EXISTS `actor` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `tmdb_id` BIGINT UNIQUE,
  `name` VARCHAR(255) NOT NULL,
  `profile_path` TEXT,
  `photo_local_path` TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Director
CREATE TABLE IF NOT EXISTS `director` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `tmdb_id` BIGINT UNIQUE,
  `name` VARCHAR(255) NOT NULL,
  `profile_path` TEXT,
  `photo_local_path` TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Relaciones pelicula_actor y pelicula_director
CREATE TABLE IF NOT EXISTS `pelicula_actor` (
  `pelicula_id` BIGINT NOT NULL,
  `actor_id` BIGINT NOT NULL,
  PRIMARY KEY (`pelicula_id`,`actor_id`),
  CONSTRAINT `fk_pelicula_actor_pelicula` FOREIGN KEY (`pelicula_id`) REFERENCES `pelicula`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pelicula_actor_actor` FOREIGN KEY (`actor_id`) REFERENCES `actor`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `pelicula_director` (
  `pelicula_id` BIGINT NOT NULL,
  `director_id` BIGINT NOT NULL,
  PRIMARY KEY (`pelicula_id`,`director_id`),
  CONSTRAINT `fk_pelicula_director_pelicula` FOREIGN KEY (`pelicula_id`) REFERENCES `pelicula`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pelicula_director_director` FOREIGN KEY (`director_id`) REFERENCES `director`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Categorias
CREATE TABLE IF NOT EXISTS `categoria` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `pelicula_categoria` (
  `pelicula_id` BIGINT NOT NULL,
  `categoria_id` BIGINT NOT NULL,
  PRIMARY KEY (`pelicula_id`,`categoria_id`),
  CONSTRAINT `fk_pelicula_categoria_pelicula` FOREIGN KEY (`pelicula_id`) REFERENCES `pelicula`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pelicula_categoria_categoria` FOREIGN KEY (`categoria_id`) REFERENCES `categoria`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Reseñas
CREATE TABLE IF NOT EXISTS `resena` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `usuario_id` BIGINT NOT NULL,
  `pelicula_id` BIGINT NOT NULL,
  `texto` TEXT,
  `puntuacion` TINYINT NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `fk_resena_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuario`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_resena_pelicula` FOREIGN KEY (`pelicula_id`) REFERENCES `pelicula`(`id`) ON DELETE CASCADE,
  UNIQUE KEY `uk_resena_usuario_pelicula` (`usuario_id`,`pelicula_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Valoración de reseñas (likes/dislikes)
CREATE TABLE IF NOT EXISTS `valoracion_resena` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `usuario_id` BIGINT NOT NULL,
  `resena_id` BIGINT NOT NULL,
  `tipo` TINYINT NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_valoracion_resena_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuario`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_valoracion_resena_resena` FOREIGN KEY (`resena_id`) REFERENCES `resena`(`id`) ON DELETE CASCADE,
  UNIQUE KEY `uk_valoracion_usuario_resena` (`usuario_id`,`resena_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Comentarios (opcional)
CREATE TABLE IF NOT EXISTS `comentario` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `usuario_id` BIGINT NOT NULL,
  `pelicula_id` BIGINT NOT NULL,
  `texto` TEXT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_comentario_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuario`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comentario_pelicula` FOREIGN KEY (`pelicula_id`) REFERENCES `pelicula`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Refresh tokens
CREATE TABLE IF NOT EXISTS `refresh_token` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `usuario_id` BIGINT NOT NULL,
  `token` VARCHAR(512) NOT NULL UNIQUE,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `expiry_date` DATETIME,
  CONSTRAINT `fk_refresh_token_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuario`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Crear índices de forma segura comprobando INFORMATION_SCHEMA (procedimiento temporal)
DELIMITER $$
CREATE PROCEDURE IF NOT EXISTS add_index_if_missing()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='resena' AND INDEX_NAME='idx_resena_usuario') THEN
    ALTER TABLE resena ADD INDEX idx_resena_usuario (usuario_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='resena' AND INDEX_NAME='idx_resena_pelicula') THEN
    ALTER TABLE resena ADD INDEX idx_resena_pelicula (pelicula_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pelicula' AND INDEX_NAME='idx_pelicula_tmdb') THEN
    ALTER TABLE pelicula ADD INDEX idx_pelicula_tmdb (tmdb_id);
  END IF;
END$$
CALL add_index_if_missing();
DROP PROCEDURE IF EXISTS add_index_if_missing;
DELIMITER ;

SELECT 'OK - tablas creadas o ya existentes' AS status;
