plugins {
  id("kotlin-library-conventions")
}

dependencies {
  implementation(projects.stoikExposedCore)
  implementation("com.google.devtools.ksp:symbol-processing-api:1.6.0-1.0.1")
  implementation("com.squareup:kotlinpoet:1.10.2")
}
