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

sourdough {
  libraryName.set("Lerasium Core")
  libraryDescription.set("The absolute essence of Lerasium")
}

dependencies {
  api(group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-core", version = "1.3.2")
  api(group = "io.konform", name = "konform-jvm", version = "0.3.0")
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
