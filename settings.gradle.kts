rootProject.name = "lerasium"

// CORE
include("core")
include("core-processor")

// Exposed (RDBMS)
include("rdbms")
include("rdbms-processor")

// KMongo (NoSQL)
include("mongo")
include("mongo-processor")

// Ktor (API)
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
