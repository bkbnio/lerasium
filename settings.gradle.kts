rootProject.name = "stoik"

include("stoik-exposed-core")
include("stoik-exposed-processor")
include("stoik-ktor-core")
include("stoik-ktor-processor")
include("stoik-playground")

// Feature Previews
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
