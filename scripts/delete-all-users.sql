-- Script para eliminar todos los usuarios de PelisApp
-- EJECUTAR CON CUIDADO - Esta acción es irreversible

USE pelisapp;

-- Deshabilitar verificaciones de foreign key temporalmente
SET foreign_key_checks = 0;

-- Mostrar usuarios existentes antes de borrar
SELECT 'USUARIOS ANTES DEL BORRADO:' as info;
SELECT id, username, email, registered_at, email_confirmed FROM usuario;

-- Borrar datos relacionados primero para evitar errores de foreign key

-- 1. Borrar moderaciones de comentarios relacionadas con reseñas de usuarios
DELETE cm FROM comment_moderation cm
JOIN review r ON cm.review_id = r.id
WHERE r.user_id IS NOT NULL;

-- 2. Borrar likes de reseñas
DELETE FROM review_like WHERE user_id IS NOT NULL;

-- 3. Borrar reseñas
DELETE FROM review WHERE user_id IS NOT NULL;

-- 4. Borrar logros/achievements de usuarios
DELETE FROM usuario_archivement WHERE usuario_id IS NOT NULL;

-- 5. Borrar relaciones usuario-rol
DELETE FROM usuario_roles WHERE usuario_id IS NOT NULL;

-- 6. Borrar relaciones usuario-tag
DELETE FROM usuario_etiquetas WHERE usuario_id IS NOT NULL;

-- 7. Finalmente, borrar todos los usuarios
DELETE FROM usuario;

-- Mostrar resultado
SELECT 'USUARIOS DESPUÉS DEL BORRADO:' as info;
SELECT COUNT(*) as total_usuarios_restantes FROM usuario;

-- Re-habilitar verificaciones de foreign key
SET foreign_key_checks = 1;

-- Mostrar resumen
SELECT
    'OPERACIÓN COMPLETADA' as status,
    NOW() as timestamp,
    'Todos los usuarios han sido eliminados' as mensaje;
