plugins {
  kotlin("jvm")
  id("io.bkbn.sourdough.library.jvm")
  id("io.gitlab.arturbosch.detekt")
  id("org.jetbrains.kotlinx.kover")
  id("com.adarshr.test-logger")
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

  // Lerasium
  api(projects.lerasiumCore)

  // Ktor
  api("io.ktor:ktor-server-core:$ktorVersion")
  api("io.ktor:ktor-server-cio:$ktorVersion")
  api("io.ktor:ktor-server-auth:$ktorVersion")
  api("io.ktor:ktor-server-auth-jwt:$ktorVersion")
  api("io.ktor:ktor-server-html-builder:$ktorVersion")
  api("io.ktor:ktor-server-content-negotiation:$ktorVersion")
  api("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

  // Kompendium
  api("io.bkbn:kompendium-core:$kompendiumVersion")
}
