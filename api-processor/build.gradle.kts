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
  libraryName.set("Lerasium Api Processor")
  libraryDescription.set("Annotation processor for API annotations️")
  compilerArgs.set(listOf("-opt-in=kotlin.RequiresOptIn"))
}


dependencies {
  // IMPLEMENTATION

  // Versions
  val kspVersion: String by project
  val kotlinPoetVersion: String by project

  // Lerasium
  implementation(projects.lerasiumCore)
  implementation(projects.lerasiumUtils)
  implementation(projects.lerasiumApi)

  // Symbol Processing
  implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")

  // CodeGen
  implementation("com.squareup:kotlinpoet:$kotlinPoetVersion")
  implementation("com.squareup:kotlinpoet-ksp:$kotlinPoetVersion")

  // Test Utils
  testImplementation(testFixtures(projects.lerasiumUtils))
  testImplementation(kotlin("script-runtime"))
}

testing {
  suites {
    named("test", JvmTestSuite::class) {
      useJUnitJupiter()
    }
  }
}
