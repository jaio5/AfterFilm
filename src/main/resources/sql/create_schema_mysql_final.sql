-- AfterFilm: esquema final para MySQL 8+ (no destructivo)
-- UTF8MB4, compatible con JPA/Hibernate IDENTITY (AUTO_INCREMENT)
-- Ejecutar con privilegios suficientes. Hacer backup antes de aplicar en producción.

CREATE DATABASE IF NOT EXISTS `AfterFilm` CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
USE `AfterFilm`;

-- Tabla usuario (coincide con entidad User @Table("usuario"))
CREATE TABLE IF NOT EXISTS `usuario` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(100) NOT NULL UNIQUE,
  `email` VARCHAR(200),
  `password` VARCHAR(255) NOT NULL,
  `display_name` VARCHAR(255),
  `profile_image_path` VARCHAR(1000),
  `registered_at` DATETIME,
  `critic_level` INT NOT NULL DEFAULT 0,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  INDEX (`username`),
  INDEX (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Roles
CREATE TABLE IF NOT EXISTS `role` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL UNIQUE,
  `description` TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Usuario -> roles (join)
CREATE TABLE IF NOT EXISTS `usuario_roles` (
  `usuario_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  PRIMARY KEY (`usuario_id`,`role_id`),
  INDEX (`role_id`),
  CONSTRAINT `fk_usuario_roles_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuario`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_usuario_roles_role` FOREIGN KEY (`role_id`) REFERENCES `role`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tag (etiquetas / badges)
CREATE TABLE IF NOT EXISTS `tag` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `code` VARCHAR(100) NOT NULL UNIQUE,
  `name` VARCHAR(255) NOT NULL,
  `description` TEXT,
  `icon_url` VARCHAR(512),
  `created_at` DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Usuario -> tags
CREATE TABLE IF NOT EXISTS `usuario_etiquetas` (
  `usuario_id` BIGINT NOT NULL,
  `etiqueta_id` BIGINT NOT NULL,
  PRIMARY KEY (`usuario_id`,`etiqueta_id`),
  CONSTRAINT `fk_usuario_etiquetas_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuario`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_usuario_etiquetas_tag` FOREIGN KEY (`etiqueta_id`) REFERENCES `tag`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Movie (coincide con entidad Movie)
CREATE TABLE IF NOT EXISTS `movie` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `tmdb_id` BIGINT UNIQUE,
  `title` VARCHAR(255) NOT NULL,
  `description` TEXT,
  `release_date` DATE,
  `runtime_minutes` INT,
  `poster_path` VARCHAR(1000),
  `poster_local_path` VARCHAR(1000),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX (`title`),
  INDEX (`tmdb_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Actor
CREATE TABLE IF NOT EXISTS `actor` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `tmdb_id` BIGINT UNIQUE,
  `name` VARCHAR(255) NOT NULL,
  `profile_path` VARCHAR(1000),
  `photo_local_path` VARCHAR(1000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Director
CREATE TABLE IF NOT EXISTS `director` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `tmdb_id` BIGINT UNIQUE,
  `name` VARCHAR(255) NOT NULL,
  `profile_path` VARCHAR(1000),
  `photo_local_path` VARCHAR(1000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- movie_actor (join)
CREATE TABLE IF NOT EXISTS `movie_actor` (
  `movie_id` BIGINT NOT NULL,
  `actor_id` BIGINT NOT NULL,
  PRIMARY KEY (`movie_id`,`actor_id`),
  CONSTRAINT `fk_movie_actor_movie` FOREIGN KEY (`movie_id`) REFERENCES `movie`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_movie_actor_actor` FOREIGN KEY (`actor_id`) REFERENCES `actor`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- movie_director (join)
CREATE TABLE IF NOT EXISTS `movie_director` (
  `movie_id` BIGINT NOT NULL,
  `director_id` BIGINT NOT NULL,
  PRIMARY KEY (`movie_id`,`director_id`),
  CONSTRAINT `fk_movie_director_movie` FOREIGN KEY (`movie_id`) REFERENCES `movie`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_movie_director_director` FOREIGN KEY (`director_id`) REFERENCES `director`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Category
CREATE TABLE IF NOT EXISTS `category` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- movie_category (join)
CREATE TABLE IF NOT EXISTS `movie_category` (
  `movie_id` BIGINT NOT NULL,
  `category_id` BIGINT NOT NULL,
  PRIMARY KEY (`movie_id`,`category_id`),
  CONSTRAINT `fk_movie_category_movie` FOREIGN KEY (`movie_id`) REFERENCES `movie`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_movie_category_category` FOREIGN KEY (`category_id`) REFERENCES `category`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Review (coincide con entidad Review)
CREATE TABLE IF NOT EXISTS `review` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `movie_id` BIGINT NOT NULL,
  `text` TEXT,
  `stars` DECIMAL(2,1) NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `likes_count` BIGINT NOT NULL DEFAULT 0,
  UNIQUE KEY `uk_review_user_movie` (`user_id`,`movie_id`),
  INDEX `idx_review_user` (`user_id`),
  INDEX `idx_review_movie` (`movie_id`),
  CONSTRAINT `fk_review_user` FOREIGN KEY (`user_id`) REFERENCES `usuario`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_review_movie` FOREIGN KEY (`movie_id`) REFERENCES `movie`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Review likes (review_like)
CREATE TABLE IF NOT EXISTS `review_like` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `review_id` BIGINT NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_review_like_user_review` (`user_id`,`review_id`),
  CONSTRAINT `fk_review_like_user` FOREIGN KEY (`user_id`) REFERENCES `usuario`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_review_like_review` FOREIGN KEY (`review_id`) REFERENCES `review`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Comment moderation (nueva tabla para sistema de moderación con IA)
CREATE TABLE IF NOT EXISTS `comment_moderation` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `review_id` BIGINT NOT NULL UNIQUE,
  `status` ENUM('PENDING', 'APPROVED', 'REJECTED', 'MANUAL_REVIEW') NOT NULL DEFAULT 'PENDING',
  `toxicity_score` DECIMAL(5,3),
  `moderation_reason` VARCHAR(1000),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `reviewed_at` DATETIME,
  `reviewed_by` BIGINT,
  `ai_processed` TINYINT(1) DEFAULT 0,
  INDEX (`status`),
  INDEX (`created_at`),
  CONSTRAINT `fk_comment_moderation_review` FOREIGN KEY (`review_id`) REFERENCES `review`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comment_moderation_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `usuario`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Archivement (logros)
CREATE TABLE IF NOT EXISTS `archivement` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `code` VARCHAR(100) NOT NULL UNIQUE,
  `name` VARCHAR(255),
  `description` TEXT,
  `icon_url` VARCHAR(512),
  `created_at` DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Usuario archivement
CREATE TABLE IF NOT EXISTS `usuario_archivement` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `usuario_id` BIGINT NOT NULL,
  `archivement_id` BIGINT NOT NULL,
  `awarded_at` DATETIME,
  `pinned_to_profile` TINYINT(1) DEFAULT 0,
  UNIQUE KEY `uk_usuario_archivement` (`usuario_id`,`archivement_id`),
  CONSTRAINT `fk_usuario_archivement_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuario`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_usuario_archivement_archivement` FOREIGN KEY (`archivement_id`) REFERENCES `archivement`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Following (seguidores)
CREATE TABLE IF NOT EXISTS `following` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `follower_id` BIGINT NOT NULL,
  `followed_id` BIGINT NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_following` (`follower_id`,`followed_id`),
  CONSTRAINT `fk_following_follower` FOREIGN KEY (`follower_id`) REFERENCES `usuario`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_following_followed` FOREIGN KEY (`followed_id`) REFERENCES `usuario`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Agregar índices adicionales si faltan (procedimiento temporal, compatible con MySQL que no soporta CREATE INDEX IF NOT EXISTS)
DELIMITER $$
CREATE PROCEDURE IF NOT EXISTS add_index_if_missing()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='review' AND INDEX_NAME='idx_review_user') THEN
    ALTER TABLE review ADD INDEX idx_review_user (user_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='review' AND INDEX_NAME='idx_review_movie') THEN
    ALTER TABLE review ADD INDEX idx_review_movie (movie_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='movie' AND INDEX_NAME='idx_movie_tmdb') THEN
    ALTER TABLE movie ADD INDEX idx_movie_tmdb (tmdb_id);
  END IF;
END$$
CALL add_index_if_missing();
DROP PROCEDURE IF EXISTS add_index_if_missing;
DELIMITER ;

SELECT 'OK - esquema final creado o ya existente' AS status;

-- Datos iniciales de logros
INSERT IGNORE INTO `archivement` (`code`, `name`, `description`, `created_at`) VALUES
('FIRST_REVIEW', 'Primera Reseña', 'Escribió su primera reseña de película', NOW()),
('REVIEWER_10', 'Crítico Novato', 'Escribió 10 reseñas de películas', NOW()),
('REVIEWER_50', 'Crítico Experimentado', 'Escribió 50 reseñas de películas', NOW()),
('CRITIC_100', 'Crítico Profesional', 'Escribió 100 reseñas de películas', NOW()),
('FIRST_LIKE', 'Primer Like', 'Recibió su primer like en una reseña', NOW()),
('POPULAR_25', 'Popular', 'Recibió 25 likes en total en sus reseñas', NOW()),
('INFLUENCER_100', 'Influencer', 'Recibió 100 likes en total en sus reseñas', NOW()),
('VIRAL_REVIEW', 'Review Viral', 'Una de sus reseñas recibió 20+ likes', NOW()),
('POPULAR_10_FOLLOWERS', 'Seguido', 'Tiene 10 seguidores', NOW()),
('CELEBRITY_50_FOLLOWERS', 'Celebridad', 'Tiene 50 seguidores', NOW());
