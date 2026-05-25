CREATE TABLE IF NOT EXISTS review_reply (
  id BIGSERIAL PRIMARY KEY,
  review_id BIGINT NOT NULL REFERENCES review(id) ON DELETE CASCADE,
  user_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
  text VARCHAR(1000) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS idx_review_reply_review ON review_reply(review_id);
CREATE INDEX IF NOT EXISTS idx_review_reply_user ON review_reply(user_id);
CREATE INDEX IF NOT EXISTS idx_review_reply_created ON review_reply(created_at);
