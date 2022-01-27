rootProject.name = "stoik"

// CORE
include("core")
include("core-processor")

// RDBMS
include("exposed")
include("exposed-processor")

// API
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
