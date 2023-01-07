plugins {
  kotlin("jvm")
  id("io.bkbn.sourdough.library.jvm")
  id("io.gitlab.arturbosch.detekt")
  id("org.jetbrains.kotlinx.kover")
  id("com.adarshr.test-logger")
  id("maven-publish")
  id("java-library")
  id("signing")
}

sourdoughLibrary {
  libraryName.set("Lerasium RDBMS Core")
  libraryDescription.set("Collection of annotations for driving RDBMS table generation️")
}


dependencies {
  // IMPLEMENTATION

  // Versions
  val exposedVersion: String by project
  val hikariCPVersion: String by project
  val flywayVersion: String by project
  val postgresVersion: String by project

  // Lerasium
  api(projects.lerasiumPersistence)

  // Exposed
  api("org.jetbrains.exposed:exposed-core:$exposedVersion")
  api("org.jetbrains.exposed:exposed-dao:$exposedVersion")
  api("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
  api("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")

  // Database
  api("org.flywaydb:flyway-core:$flywayVersion")
  api("com.zaxxer:HikariCP:$hikariCPVersion")
  api("org.postgresql:postgresql:$postgresVersion")
}
