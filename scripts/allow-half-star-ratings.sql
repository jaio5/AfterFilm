ALTER TABLE review
  ALTER COLUMN stars TYPE NUMERIC(2,1) USING stars::numeric;
