-- Lists deprecated objects that are safe candidates for cleanup in PostgreSQL.
-- Run before cleanup_deprecated_postgres.sql.

SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN (
      'comments',
      'score',
      'country',
      'movie_actor_role',
      'comentary_moderation',
      'refresh_token',
      'pelicula',
      'resena',
      'valoracion_resena',
      'pelicula_categoria',
      'pelicula_actor',
      'pelicula_director',
      'etiqueta'
  )
ORDER BY table_name;

SELECT table_name, column_name
FROM information_schema.columns
WHERE table_schema = 'public'
  AND (
      (table_name IN ('actor', 'director') AND column_name = 'photo_local_path')
  )
ORDER BY table_name, column_name;

SELECT
    c.relname AS table_name,
    c.reltuples::bigint AS estimated_rows
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public'
  AND c.relkind = 'r'
  AND c.relname IN ('comments', 'score', 'country', 'movie_actor_role')
ORDER BY c.relname;
