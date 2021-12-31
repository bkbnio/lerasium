CREATE TABLE IF NOT EXISTS "user"
(
  id         uuid PRIMARY KEY,
  first_name VARCHAR(128) NOT NULL,
  last_name  VARCHAR(128) NOT NULL,
  email      VARCHAR(128) NOT NULL
--   created_at TIMESTAMP    NOT NULL,
--   updated_at TIMESTAMP    NOT NULL
);

ALTER TABLE "user"
  ADD CONSTRAINT user_email_unique UNIQUE (email);
