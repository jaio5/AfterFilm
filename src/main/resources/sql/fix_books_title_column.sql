-- Fix for Books title column truncation error
-- This script increases the title column from VARCHAR(255) to VARCHAR(500)
-- to support longer book titles from Google Books API

USE `AfterFilm`;

-- Create the books table if it doesn't exist with proper column size
CREATE TABLE IF NOT EXISTS `books` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `google_books_id` VARCHAR(255) UNIQUE,
  `isbn` VARCHAR(50),
  `title` VARCHAR(500) NOT NULL,
  `authors` TEXT,
  `publisher` VARCHAR(255),
  `published_date` VARCHAR(50),
  `description` TEXT,
  `page_count` INT,
  `categories` TEXT,
  `language` VARCHAR(10),
  `cover_url` VARCHAR(1000),
  INDEX idx_title (title),
  INDEX idx_google_books_id (google_books_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- If the table already exists with VARCHAR(255), alter it
-- Note: This is safe only if the table is not being actively written to
ALTER TABLE `books` 
  MODIFY COLUMN `title` VARCHAR(500) NOT NULL,
  MODIFY COLUMN `authors` TEXT,
  MODIFY COLUMN `description` TEXT,
  MODIFY COLUMN `categories` TEXT;

SELECT 'OK - books table schema fixed' AS status;
