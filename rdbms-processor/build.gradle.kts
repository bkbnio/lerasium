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
  libraryName.set("Lerasium RDBMS Processor")
  libraryDescription.set("Annotation processor for the Lerasium RDBMS Integration")
  compilerArgs.set(listOf("-opt-in=kotlin.RequiresOptIn"))
}


dependencies {
  // IMPLEMENTATION

  // Versions
  val kotlinPoetVersion: String by project
  val kspVersion: String by project
  val kotlinxDatetimeVersion: String by project

  // Lerasium
  implementation(projects.lerasiumRdbms)
  implementation(projects.lerasiumUtils)
  implementation(projects.lerasiumCore)

  // CodeGen
  implementation("com.squareup:kotlinpoet:$kotlinPoetVersion")
  implementation("com.squareup:kotlinpoet-ksp:$kotlinPoetVersion")

  // Symbol Processing
  implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")

  // Date
  implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDatetimeVersion")

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
