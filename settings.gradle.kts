rootProject.name = "lerasium"

// CORE
include("core")
include("core-processor")

// Persistence TODO Remove?
include("persistence")

// RDBMS
include("rdbms")
include("rdbms-processor")

// Mongo
include("mongo")
include("mongo-processor")

// API
include("api")
include("api-processor")

// Playground
include("playground")

// Utility
include("utils")


run {
  rootProject.children.forEach { it.name = "${rootProject.name}-${it.name}" }
}

// Feature Previews
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
