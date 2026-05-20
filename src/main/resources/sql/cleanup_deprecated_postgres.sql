-- AfterFilm / PelisApp cleanup for deprecated PostgreSQL objects.
-- Target: Supabase/PostgreSQL.
--
-- Run this manually in a maintenance window after making a database backup.
-- The app no longer maps or uses these tables/columns.
--
-- Deprecated tables removed from the JPA model:
--   comments          -> replaced by review.text/stars/user/content relations
--   score             -> replaced by review.stars
--   country           -> unused placeholder entity
--   movie_actor_role  -> unused role table; current cast relation is movie_actor
--
-- Legacy objects from older MySQL-era scripts, if they exist:
--   comentary_moderation -> replaced by comment_moderation
--   refresh_token        -> no current entity/service uses persistent refresh tokens
--   pelicula/resena/valoracion_resena/etiqueta -> renamed legacy tables
--   actor.photo_local_path/director.photo_local_path -> replaced by profile_local_path

BEGIN;

CREATE SCHEMA IF NOT EXISTS deprecated_backup;

-- Preserve any remaining data before dropping. These backups can be removed
-- later after verifying production works normally.
DO $$
DECLARE
    table_name text;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'comments',
        'score',
        'country',
        'movie_actor_role',
        'comentary_moderation',
        'refresh_token'
    ]
    LOOP
        IF to_regclass('public.' || table_name) IS NOT NULL THEN
            EXECUTE format(
                'CREATE TABLE IF NOT EXISTS deprecated_backup.%I AS SELECT * FROM public.%I',
                table_name,
                table_name
            );
        END IF;
    END LOOP;
END $$;

-- Copy old local image paths into the currently used columns before removing
-- the old names, if both columns exist.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'actor' AND column_name = 'photo_local_path'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'actor' AND column_name = 'profile_local_path'
    ) THEN
        UPDATE public.actor
        SET profile_local_path = photo_local_path
        WHERE (profile_local_path IS NULL OR profile_local_path = '')
          AND photo_local_path IS NOT NULL
          AND photo_local_path <> '';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'director' AND column_name = 'photo_local_path'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'director' AND column_name = 'profile_local_path'
    ) THEN
        UPDATE public.director
        SET profile_local_path = photo_local_path
        WHERE (profile_local_path IS NULL OR profile_local_path = '')
          AND photo_local_path IS NOT NULL
          AND photo_local_path <> '';
    END IF;
END $$;

ALTER TABLE IF EXISTS public.actor DROP COLUMN IF EXISTS photo_local_path;
ALTER TABLE IF EXISTS public.director DROP COLUMN IF EXISTS photo_local_path;

DROP TABLE IF EXISTS public.movie_actor_role CASCADE;
DROP TABLE IF EXISTS public.comments CASCADE;
DROP TABLE IF EXISTS public.score CASCADE;
DROP TABLE IF EXISTS public.country CASCADE;

DROP TABLE IF EXISTS public.comentary_moderation CASCADE;
DROP TABLE IF EXISTS public.refresh_token CASCADE;

-- Old Spanish/MySQL names from the legacy schema. These should only exist if
-- an old migration left duplicate tables behind.
DROP TABLE IF EXISTS public.valoracion_resena CASCADE;
DROP TABLE IF EXISTS public.resena CASCADE;
DROP TABLE IF EXISTS public.pelicula_categoria CASCADE;
DROP TABLE IF EXISTS public.pelicula_actor CASCADE;
DROP TABLE IF EXISTS public.pelicula_director CASCADE;
DROP TABLE IF EXISTS public.pelicula CASCADE;
DROP TABLE IF EXISTS public.etiqueta CASCADE;

COMMIT;
