plugins {
  kotlin("jvm")
  id("io.bkbn.sourdough.library.jvm")
  id("io.gitlab.arturbosch.detekt")
  id("com.adarshr.test-logger")
  id("maven-publish")
  id("java-library")
  id("signing")
}

sourdoughLibrary {
  libraryName.set("Lerasium Mongo Core")
  libraryDescription.set("Collection of annotations for driving Mongo document persistence")
}

dependencies {
  // IMPLEMENTATION
  val kmongoVersion: String by project

  // Lerasium
  api(projects.lerasiumPersistence)

  // KMongo
  api("org.litote.kmongo:kmongo-serialization:$kmongoVersion")
}
