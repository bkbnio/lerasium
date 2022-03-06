CREATE TABLE IF NOT EXISTS author (id uuid PRIMARY KEY, "name" VARCHAR(128) NOT NULL, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL);
CREATE TABLE IF NOT EXISTS book (id uuid PRIMARY KEY, isbn VARCHAR(128) NOT NULL, author uuid NOT NULL, title VARCHAR(128) NOT NULL, rating DOUBLE PRECISION NOT NULL, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL, CONSTRAINT fk_book_author__id FOREIGN KEY (author) REFERENCES author(id) ON DELETE RESTRICT ON UPDATE RESTRICT);
ALTER TABLE book ADD CONSTRAINT book_isbn_unique UNIQUE (isbn);
CREATE TABLE IF NOT EXISTS "user" (id uuid PRIMARY KEY, email VARCHAR(128) NOT NULL, first_name VARCHAR(128) NOT NULL, last_name VARCHAR(128) NOT NULL, favorite_food VARCHAR(128) NULL, "password" VARCHAR(128) NOT NULL, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL);
ALTER TABLE "user" ADD CONSTRAINT user_email_unique UNIQUE (email);
CREATE INDEX user_first_name_last_name ON "user" (first_name, last_name);
CREATE INDEX user_favorite_food_last_name ON "user" (favorite_food, last_name);
CREATE TABLE IF NOT EXISTS book_review (id uuid PRIMARY KEY, reader uuid NOT NULL, book uuid NOT NULL, rating INT NOT NULL, review VARCHAR(128) NOT NULL, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL, CONSTRAINT fk_book_review_reader__id FOREIGN KEY (reader) REFERENCES "user"(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_book_review_book__id FOREIGN KEY (book) REFERENCES book(id) ON DELETE RESTRICT ON UPDATE RESTRICT);
ALTER TABLE book_review ADD CONSTRAINT book_review_reader_book_unique UNIQUE (reader, book);
