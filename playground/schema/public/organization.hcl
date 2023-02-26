table "organization" {
  schema = schema.public
  column "id" {
    null = false
    type = uuid
  }

  column "name" {
    null = false
    type = varchar(100)
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

  index "idx_organization_name" {
    columns = [column.name]
    unique  = true
  }
}
