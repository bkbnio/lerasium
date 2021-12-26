plugins {
  id("io.bkbn.sourdough.library")
}

dependencies {
  // IMPLEMENTATION

  // Ktor
  api(group = "io.ktor", name = "ktor-server-core", version = "1.6.7")
  api(group = "io.ktor", name = "ktor-server-netty", version = "1.6.7")
  api(group = "io.ktor", name = "ktor-serialization", version = "1.6.7")
}
