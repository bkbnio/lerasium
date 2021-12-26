plugins {
  id("io.bkbn.sourdough.library")
}

dependencies {
  val exposedVersion = "0.36.2"
  api("org.jetbrains.exposed:exposed-core:$exposedVersion")
  api("org.jetbrains.exposed:exposed-dao:$exposedVersion")
  api("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
}
