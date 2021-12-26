plugins {
  id("io.bkbn.sourdough.library")
}

dependencies {
  implementation(projects.stoikKtorCore)
  implementation("com.google.devtools.ksp:symbol-processing-api:1.6.0-1.0.1")
  implementation("com.squareup:kotlinpoet:1.10.2")
}
