table "users" {
  schema = schema.public

  column "id" {
    null = false
    type = uuid
  }

  column "email" {
    null = false
    type = varchar(255)
  }

  column "password" {
    null = false
    type = varchar(255)
  }

  column "created_at" {
    null = false
    type = timestamp
  }

  column "updated_at" {
    null = false
    type = timestamp
  }

  column "version" {
    null    = false
    type    = int
    default = 0
  }

  primary_key {
    columns = [column.id]
  }

  index "idx_user_email" {
    columns = [column.email]
    unique  = true
  }
}
