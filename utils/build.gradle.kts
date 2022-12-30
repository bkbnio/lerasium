plugins {
  kotlin("jvm")
  id("io.bkbn.sourdough.library.jvm")
  id("io.gitlab.arturbosch.detekt")
  id("org.jetbrains.kotlinx.kover")
  id("com.adarshr.test-logger")
  id("maven-publish")
  id("java-library")
  id("java-test-fixtures")
  id("signing")
}

sourdoughLibrary {
  libraryName.set("Lerasium Utils")
  libraryDescription.set("Collection of utilities for use across all modules️")
  compilerArgs.set(listOf("-opt-in=kotlin.RequiresOptIn"))
}

dependencies {
  // IMPLEMENTATION

  // Versions
  val kotlinPoetVersion: String by project
  val kspVersion: String by project

  // Lerasium
  implementation(projects.lerasiumCore)

  // CodeGen
  implementation("com.squareup:kotlinpoet:$kotlinPoetVersion")
  implementation("com.squareup:kotlinpoet-ksp:$kotlinPoetVersion")

  // Symbol Processing
  implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")

  // TESTING

  // Versions
  val kotestVersion: String by project
  val kotlinCompileTestingKspVersion: String by project

  // Fixture Libraries

  testFixturesApi("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
  testFixturesApi("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
  testFixturesApi("io.kotest.extensions:kotest-assertions-compiler:1.0.0")
  testFixturesApi("com.github.tschuchortdev:kotlin-compile-testing-ksp:$kotlinCompileTestingKspVersion")
}
