rootProject.name = "stoik"

//

// RDBMS
include("stoik-exposed-core")
include("stoik-exposed-processor")

// API
include("stoik-ktor-core")
include("stoik-ktor-processor")

// Playground
include("stoik-playground")

// Utility
include("stoik-utils")

// Feature Previews
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
