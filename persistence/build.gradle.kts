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
  libraryName.set("Lerasium Persistence")
  libraryDescription.set("Collection of annotations useful across persistence mechanisms")
}

dependencies {
  // IMPLEMENTATION

  // KMongo
  api(group = "org.litote.kmongo", name = "kmongo-serialization", version = "4.4.0")
}
