CREATE TABLE IF NOT EXISTS comments (
  id BIGSERIAL PRIMARY KEY,
  event_id BIGINT NOT NULL,
  author_id BIGINT NOT NULL,
  text TEXT NOT NULL,
  created_on TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_on TIMESTAMP,
  CONSTRAINT fk_comments_event  FOREIGN KEY (event_id)  REFERENCES events(id) ON DELETE CASCADE,
  CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users(id)  ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_comments_event_created ON comments(event_id, created_on DESC);