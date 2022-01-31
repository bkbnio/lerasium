rootProject.name = "stoik"

// CORE
include("core")
include("core-processor")

// Exposed (RDBMS)
include("exposed")
include("exposed-processor")

// KMongo (NoSQL)
include("kmongo")
include("kmongo-processor")

// Ktor (API)
include("ktor")
include("ktor-processor")

// Playground
include("playground")

// Utility
include("utils")


run {
  rootProject.children.forEach { it.name = "${rootProject.name}-${it.name}" }
}

// Feature Previews
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
