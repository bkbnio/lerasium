plugins {
  kotlin("jvm")
  id("io.bkbn.sourdough.library.jvm")
  id("io.gitlab.arturbosch.detekt")
  id("com.adarshr.test-logger")
  id("org.jetbrains.dokka")
  id("maven-publish")
  id("java-library")
  id("signing")
}

sourdough {
  libraryName.set("Stoik Ktor Core")
  libraryDescription.set("Collection of annotations for driving API generation️")
}


dependencies {
  // IMPLEMENTATION

  // Ktor
  api(group = "io.ktor", name = "ktor-server-core", version = "1.6.7")
  api(group = "io.ktor", name = "ktor-server-netty", version = "1.6.7")
  api(group = "io.ktor", name = "ktor-serialization", version = "1.6.7")
}
