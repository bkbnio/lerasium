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
  val flywayVersion: String by project
  val postgresVersion: String by project
  val komapperVersion: String by project
  val kotlinxDatetimeVersion: String by project

  // Lerasium
  api(projects.lerasiumPersistence)

  // Date
  api("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDatetimeVersion")

  // Komapper
  api(platform("org.komapper:komapper-platform:$komapperVersion"))
  api("org.komapper:komapper-starter-r2dbc")
  api("org.komapper:komapper-dialect-postgresql-r2dbc")
  api("org.komapper:komapper-datetime-r2dbc:$komapperVersion")

  // Database
  api("org.flywaydb:flyway-core:$flywayVersion")
  api("org.postgresql:postgresql:$postgresVersion")
  api("io.r2dbc:r2dbc-pool:1.0.1.RELEASE")
}
