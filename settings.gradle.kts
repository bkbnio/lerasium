rootProject.name = "stoik"

// RDBMS
include("stoik-exposed-core")
include("stoik-exposed-processor")

// DAO
include("stoik-dao-core")
include("stoik-dao-processor")

// API
include("stoik-ktor-core")
include("stoik-ktor-processor")

include("stoik-playground")

// Feature Previews
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
