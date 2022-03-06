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
  libraryName.set("Lerasium Api Processor")
  libraryDescription.set("Annotation processor for API annotations️")
  compilerArgs.set(listOf("-opt-in=kotlin.RequiresOptIn"))
}


dependencies {
  // IMPLEMENTATION

  // Lerasium
  implementation(projects.lerasiumCore)
  implementation(projects.lerasiumUtils)
  implementation(projects.lerasiumApi)

  // Symbol Processing
  implementation(group = "com.google.devtools.ksp", name = "symbol-processing-api", version = "1.6.10-1.0.4")

  // CodeGen
  implementation(group = "com.squareup", name = "kotlinpoet", version = "1.10.2")
  implementation(group = "com.squareup", name = "kotlinpoet-ksp", version = "1.10.2")

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
