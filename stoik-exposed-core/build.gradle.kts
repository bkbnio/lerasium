plugins {
  id("io.bkbn.sourdough.library")
}

dependencies {
  // IMPLEMENTATION

  // Exposed
  api(group = "org.jetbrains.exposed", name = "exposed-core", version = "0.36.2")
  api(group = "org.jetbrains.exposed", name = "exposed-dao", version = "0.36.2")
  api(group = "org.jetbrains.exposed", name = "exposed-jdbc", version = "0.36.2")
}
