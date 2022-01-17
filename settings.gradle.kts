rootProject.name = "stoik"

// CORE
include("core")

// RDBMS
include("exposed-core")
include("exposed-processor")

// API
include("ktor-core")
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
