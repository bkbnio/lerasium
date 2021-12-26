plugins {
  id("io.bkbn.sourdough.library")
}

dependencies {
  // IMPLEMENTATION

  // Stoik
  implementation(projects.stoikExposedCore)

  // KSP
  implementation(group = "com.google.devtools.ksp", name = "symbol-processing-api", version = "1.6.0-1.0.2")

  // CodeGen
  implementation(group = "com.squareup", name = "kotlinpoet", version = "1.10.2")
}
