plugins {
  kotlin("jvm")
  id("io.bkbn.sourdough.library.jvm")
  id("io.gitlab.arturbosch.detekt")
  id("com.adarshr.test-logger")
  id("org.jetbrains.dokka")
  id("maven-publish")
  id("java-library")
  id("java-test-fixtures")
  id("signing")
}

sourdough {
  libraryName.set("Lerasium Utils")
  libraryDescription.set("Collection of utilities for use across all modules️")
  compilerArgs.set(listOf("-opt-in=kotlin.RequiresOptIn"))
}

dependencies {
  implementation(projects.lerasiumCore)
  implementation(group = "com.squareup", name = "kotlinpoet", version = "1.10.2")
  implementation(group = "com.squareup", name = "kotlinpoet-ksp", version = "1.10.2")
  implementation(group = "com.google.devtools.ksp", name = "symbol-processing-api", version = "1.6.10-1.0.2")

  testFixturesApi("io.kotest:kotest-runner-junit5-jvm:5.1.0")
  testFixturesApi("io.kotest:kotest-assertions-core-jvm:5.1.0")
  testFixturesApi("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.4.7")
}
