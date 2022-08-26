plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  id("io.bkbn.sourdough.library.jvm")
  id("io.gitlab.arturbosch.detekt")
  id("com.adarshr.test-logger")
  id("org.jetbrains.dokka")
  id("maven-publish")
  id("java-library")
  id("signing")
}

sourdoughLibrary {
  libraryName.set("Lerasium Core")
  libraryDescription.set("The absolute essence of Lerasium")
}

dependencies {
  // Versions
  val kotlinxSerializationVersion: String by project
  val konformVersion: String by project

  api("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")
  api("io.konform:konform-jvm:$konformVersion")
}

testing {
  suites {
    named("test", JvmTestSuite::class) {
      useJUnitJupiter()
      dependencies {
        implementation("io.kotest:kotest-runner-junit5-jvm:5.0.3")
        implementation("io.kotest:kotest-assertions-core-jvm:5.0.3")
        implementation("io.kotest.extensions:kotest-assertions-konform-jvm:1.0.0")
      }
    }
  }
}
