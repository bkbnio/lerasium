table "repository" {
  schema = schema.public

  column "id" {
    null = false
    type = uuid
  }

  column "name" {
    null = false
    type = text
  }

  column "is_public" {
    null = false
    type = boolean
  }

  column "organization" {
    null = false
    type = uuid
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
    null = false
    type = int
  }

  foreign_key {
    columns     = [column.organization]
    ref_columns = [table.organization.column.id]
    on_update   = NO_ACTION
    on_delete   = NO_ACTION
  }
}
