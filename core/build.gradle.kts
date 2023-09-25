plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  id("io.bkbn.sourdough.library.jvm")
  id("io.gitlab.arturbosch.detekt")
  id("org.jetbrains.kotlinx.kover")
  id("com.adarshr.test-logger")
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
  api("io.bkbn:bouncer-core:0.1.1")
}

testing {
  suites {
    named("test", JvmTestSuite::class) {
      useJUnitJupiter()
      dependencies {
        // Versions
        val kotestVersion: String by project

        implementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
        implementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
        implementation("io.kotest.extensions:kotest-assertions-konform-jvm:1.1.0")
      }
    }
  }
}
