plugins {
  id("com.google.devtools.ksp") version "1.6.0-1.0.2"
  application
}

dependencies {
  ksp(projects.stoikExposedProcessor)
  implementation(projects.stoikExposedCore)

  ksp(projects.stoikKtorProcessor)
  implementation(projects.stoikKtorCore)

  // Database
  implementation(group = "org.flywaydb", name = "flyway-core", version = "8.2.3")
  implementation(group = "com.zaxxer", name = "HikariCP", version = "5.0.0")
  implementation(group = "org.postgresql", name = "postgresql", version = "42.3.1")

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
  mainClassName = "io.bkbn.stoik.playground.MainKt"
}
