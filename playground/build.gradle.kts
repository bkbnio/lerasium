plugins {
  kotlin("jvm")
  kotlin("plugin.serialization") version "1.6.10"
  id("io.bkbn.sourdough.application.jvm") version "0.6.0"
  id("com.google.devtools.ksp")
  application
}

sourdough {
  compilerArgs.set(listOf("-opt-in=kotlin.RequiresOptIn"))
}

dependencies {
  implementation(projects.lerasiumCore)

  ksp(projects.lerasiumCoreProcessor)
  implementation(projects.lerasiumCore)

  ksp(projects.lerasiumRdbmsProcessor)
  implementation(projects.lerasiumRdbms)

  ksp(projects.lerasiumApiProcessor)
  implementation(projects.lerasiumApi)

  ksp(projects.lerasiumMongoProcessor)
  implementation(projects.lerasiumMongo)

  // Database
  implementation(group = "org.flywaydb", name = "flyway-core", version = "8.2.3")
  implementation(group = "com.zaxxer", name = "HikariCP", version = "5.0.0")
  implementation(group = "org.postgresql", name = "postgresql", version = "42.3.1")

  // Date
  implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-datetime", version = "0.3.1")

  // Serialization
  implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-core", version = "1.3.2")

  // Logging
  implementation(group = "org.apache.logging.log4j", name = "log4j-api-kotlin", version = "1.1.0")
  implementation(group = "org.apache.logging.log4j", name = "log4j-api", version = "2.17.1")
  implementation(group = "org.apache.logging.log4j", name = "log4j-core", version = "2.17.1")
  implementation(group = "org.apache.logging.log4j", name = "log4j-slf4j-impl", version = "2.17.1")
}

kotlin {
  sourceSets.main {
    kotlin.srcDir("build/generated/ksp/main/kotlin")
  }
  sourceSets.test {
    kotlin.srcDir("build/generated/ksp/test/kotlin")
  }
}

application {
  @Suppress("DEPRECATION")
  mainClassName = "io.bkbn.lerasium.playground.MainKt"
}
