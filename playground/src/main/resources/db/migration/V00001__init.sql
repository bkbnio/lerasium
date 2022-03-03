CREATE TABLE IF NOT EXISTS author
(
  id         uuid PRIMARY KEY,
  "name"     VARCHAR(128) NOT NULL,
  created_at TIMESTAMP    NOT NULL,
  updated_at TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS book
(
  id         uuid PRIMARY KEY,
  isbn       VARCHAR(128)     NOT NULL,
  "name"     uuid             NOT NULL,
  title      VARCHAR(128)     NOT NULL,
  rating     DOUBLE PRECISION NOT NULL,
  created_at TIMESTAMP        NOT NULL,
  updated_at TIMESTAMP        NOT NULL,
  CONSTRAINT fk_book_name__id FOREIGN KEY ("name") REFERENCES author (id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

ALTER TABLE book
  ADD CONSTRAINT book_isbn_unique UNIQUE (isbn);

CREATE TABLE IF NOT EXISTS "user"
(
  id            uuid PRIMARY KEY,
  email         VARCHAR(128) NOT NULL,
  first_name    VARCHAR(128) NOT NULL,
  last_name     VARCHAR(128) NOT NULL,
  favorite_food VARCHAR(128) NULL,
  "password"    VARCHAR(128) NOT NULL,
  created_at    TIMESTAMP    NOT NULL,
  updated_at    TIMESTAMP    NOT NULL
);

ALTER TABLE "user"
  ADD CONSTRAINT user_email_unique UNIQUE (email);

CREATE INDEX user_first_name_last_name ON "user" (first_name, last_name);

CREATE INDEX user_favorite_food_last_name ON "user" (favorite_food, last_name);
