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
  libraryName.set("Lerasium Core Processor")
  libraryDescription.set("Processes the essential Lerasium annotations️")
  compilerArgs.set(listOf("-opt-in=kotlin.RequiresOptIn"))
}

dependencies {
  // Versions
  val kspVersion: String by project
  val kotlinPoetVersion: String by project
  val kotlinxDatetimeVersion: String by project
  val kotlinxSerializationVersion: String by project

  // Lerasium
  implementation(projects.lerasiumCore)
  implementation(projects.lerasiumUtils)

  // KSP
  implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")

  // CodeGen
  implementation("com.squareup:kotlinpoet:$kotlinPoetVersion")
  implementation("com.squareup:kotlinpoet-ksp:$kotlinPoetVersion")

  // Date
  implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDatetimeVersion")

  // Serialization
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")

  // Test Utils
  testImplementation(testFixtures(projects.lerasiumUtils))
}

testing {
  suites {
    named("test", JvmTestSuite::class) {
      useJUnitJupiter()
    }
  }
}
