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
  libraryName.set("Lerasium Permission Core")
  libraryDescription.set("Collection of annotations for establishing permissions")
}

dependencies {
  // IMPLEMENTATION

  // Versions
  val bouncerVersion: String by project

  // Bouncer
  api("io.bkbn:bouncer-core:$bouncerVersion")
}
