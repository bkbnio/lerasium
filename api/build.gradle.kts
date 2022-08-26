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

sourdoughLibrary {
  libraryName.set("Lerasium API Core")
  libraryDescription.set("Collection of annotations for driving API generation️")
}

dependencies {
  // IMPLEMENTATION

  // Versions
  val ktorVersion: String by project
  val kompendiumVersion: String by project

  // Ktor
  api("io.ktor:ktor-server-core:$ktorVersion")
  api("io.ktor:ktor-server-netty:$ktorVersion")
  api("io.ktor:ktor-serialization:$ktorVersion")

  // Kompendium
  api("io.bkbn:kompendium-core:$kompendiumVersion")
}
