table "organization_role" {
  schema = schema.public
  column "id" {
    null = false
    type = uuid
  }

  column "organization" {
    null = false
    type = uuid
  }

  column "user" {
    null = false
    type = uuid
  }

  column "role" {
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

  foreign_key {
    columns     = [column.organization]
    ref_columns = [table.organization.column.id]
    on_update   = NO_ACTION
    on_delete   = NO_ACTION
  }

  foreign_key {
    columns     = [column.user]
    ref_columns = [table.users.column.id]
    on_update   = NO_ACTION
    on_delete   = NO_ACTION
  }
}
