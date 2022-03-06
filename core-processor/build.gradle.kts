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
  libraryName.set("Lerasium Core Processor")
  libraryDescription.set("Processes the essential Lerasium annotations️")
  compilerArgs.set(listOf("-opt-in=kotlin.RequiresOptIn"))
}

dependencies {
  // Lerasium
  implementation(projects.lerasiumCore)
  implementation(projects.lerasiumUtils)

  // KSP
  implementation(group = "com.google.devtools.ksp", name = "symbol-processing-api", version = "1.6.10-1.0.4")

  // CodeGen
  implementation(group = "com.squareup", name = "kotlinpoet", version = "1.10.2")
  implementation(group = "com.squareup", name = "kotlinpoet-ksp", version = "1.10.2")

  // Date
  implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-datetime", version = "0.3.1")

  // Serialization
  implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-core", version = "1.3.2")

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
