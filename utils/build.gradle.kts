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
  libraryName.set("Stoik Utils")
  libraryDescription.set("Collection of utilities for use across all modules️")
}

dependencies {
  implementation(group = "com.squareup", name = "kotlinpoet", version = "1.10.2")
}
