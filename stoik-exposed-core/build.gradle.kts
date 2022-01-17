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
  libraryName.set("Stoik Exposed Core")
  libraryDescription.set("Collection of annotations for driving Exposed ORM generation️")
}


dependencies {
  // IMPLEMENTATION

  // Exposed
  api(group = "org.jetbrains.exposed", name = "exposed-core", version = "0.36.2")
  api(group = "org.jetbrains.exposed", name = "exposed-dao", version = "0.36.2")
  api(group = "org.jetbrains.exposed", name = "exposed-jdbc", version = "0.36.2")
}
