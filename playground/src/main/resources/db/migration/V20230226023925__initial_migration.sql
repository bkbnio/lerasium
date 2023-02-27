-- create "organization" table
CREATE TABLE "organization"
(
  "id"         uuid                   NOT NULL,
  "name"       character varying(100) NOT NULL,
  "created_at" timestamp              NOT NULL,
  "updated_at" timestamp              NOT NULL,
  "version"    integer                NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);
-- create index "idx_organization_name" to table: "organization"
CREATE UNIQUE INDEX "idx_organization_name" ON "organization" ("name");
-- create "users" table
CREATE TABLE "users"
(
  "id"         uuid                   NOT NULL,
  "email"      character varying(255) NOT NULL,
  "password"   character varying(255) NOT NULL,
  "created_at" timestamp              NOT NULL,
  "updated_at" timestamp              NOT NULL,
  "version"    integer                NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);
-- create index "idx_user_email" to table: "users"
CREATE UNIQUE INDEX "idx_user_email" ON "users" ("email");
-- create "organization_role" table
CREATE TABLE "organization_role"
(
  "id"           uuid                   NOT NULL,
  "organization" uuid                   NOT NULL,
  "user"         uuid                   NOT NULL,
  "role"         character varying(100) NOT NULL,
  "created_at"   timestamp              NOT NULL,
  "updated_at"   timestamp              NOT NULL,
  "version"      integer                NOT NULL DEFAULT 0,
  PRIMARY KEY ("id"),
  CONSTRAINT "organization_role_organization_fkey" FOREIGN KEY ("organization") REFERENCES "organization" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT "organization_role_user_fkey" FOREIGN KEY ("user") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION
);
-- create "repository" table
CREATE TABLE "repository"
(
  "id"           uuid      NOT NULL,
  "name"         text      NOT NULL,
  "is_public"    boolean   NOT NULL,
  "organization" uuid      NOT NULL,
  "created_at"   timestamp NOT NULL,
  "updated_at"   timestamp NOT NULL,
  "version"      integer   NOT NULL,
  CONSTRAINT "repository_organization_fkey" FOREIGN KEY ("organization") REFERENCES "organization" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION
);
