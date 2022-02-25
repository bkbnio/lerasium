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
  libraryName.set("Lerasium RDBMS Core")
  libraryDescription.set("Collection of annotations for driving RDBMS table generation️")
}


dependencies {
  // IMPLEMENTATION

  // Lerasium
  api(projects.lerasiumPersistence)

  // Exposed
  api(group = "org.jetbrains.exposed", name = "exposed-core", version = "0.37.3")
  api(group = "org.jetbrains.exposed", name = "exposed-dao", version = "0.37.3")
  api(group = "org.jetbrains.exposed", name = "exposed-jdbc", version = "0.37.3")
  api(group = "org.jetbrains.exposed", name = "exposed-kotlin-datetime", version = "0.37.3")
}
