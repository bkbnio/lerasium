plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  id("io.bkbn.sourdough.application.jvm")
  id("com.google.devtools.ksp")
  application
}

sourdoughApp {
  compilerArgs.set(listOf("-opt-in=kotlin.RequiresOptIn"))
}

dependencies {
  // Versions
  val flywayVersion: String by project
  val hikariCPVersion: String by project
  val postgresVersion: String by project
  val kotlinxDatetimeVersion: String by project
  val kotlinxSerializationVersion: String by project

  // Lerasium
  ksp(projects.lerasiumCoreProcessor)
  implementation(projects.lerasiumCore)

  ksp(projects.lerasiumRdbmsProcessor)
  implementation(projects.lerasiumRdbms)

  ksp(projects.lerasiumApiProcessor)
  implementation(projects.lerasiumApi)

  ksp(projects.lerasiumMongoProcessor)
  implementation(projects.lerasiumMongo)

  // Database
  implementation("org.flywaydb:flyway-core:$flywayVersion")
  implementation("com.zaxxer:HikariCP:$hikariCPVersion")
  implementation("org.postgresql:postgresql:$postgresVersion")

  // Date
  implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDatetimeVersion")

  // Serialization
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")

  // Logging (TODO Replace w/ Kermit!)
  implementation("org.apache.logging.log4j:log4j-api-kotlin:1.1.0")
  implementation("org.apache.logging.log4j:log4j-api:2.17.1")
  implementation("org.apache.logging.log4j:log4j-core:2.17.1")
  implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.17.1")
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
