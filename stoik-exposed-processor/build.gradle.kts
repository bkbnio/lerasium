plugins {
  id("io.bkbn.sourdough.library")
}

dependencies {
  // IMPLEMENTATION

  // Stoik
  implementation(projects.stoikExposedCore)

  // KSP
  implementation(group = "com.google.devtools.ksp", name = "symbol-processing-api", version = "1.6.0-1.0.2")

  // CodeGen
  implementation(group = "com.squareup", name = "kotlinpoet", version = "1.10.2")
  implementation(group = "com.squareup", name = "kotlinpoet-ksp", version = "1.10.2")
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
