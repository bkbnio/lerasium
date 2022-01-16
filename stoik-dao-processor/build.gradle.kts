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
  libraryName.set("Stoik Dao Processor")
  libraryDescription.set("DAO Annotation Processor️")
}

dependencies {
  // IMPLEMENTATION

  // Stoik
  implementation(projects.stoikDaoCore)

  // KSP
  implementation(group = "com.google.devtools.ksp", name = "symbol-processing-api", version = "1.6.10-1.0.2")

  // CodeGen
  implementation(group = "com.squareup", name = "kotlinpoet", version = "1.10.2")
  implementation(group = "com.squareup", name = "kotlinpoet-ksp", version = "1.10.2")

  // Serialization
  implementation(group = "org.jetbrains.kotlinx", "kotlinx-serialization-json", version = "1.3.2")
}

testing {
  suites {
    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter()
      dependencies {
        implementation("io.kotest:kotest-runner-junit5-jvm:5.0.3")
        implementation("io.kotest:kotest-assertions-core-jvm:5.0.3")
        implementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.4.7")
      }
    }
  }
}
