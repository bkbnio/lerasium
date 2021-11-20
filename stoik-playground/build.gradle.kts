plugins {
  id("kotlin-application-conventions")
  id("com.google.devtools.ksp") version "1.6.0-1.0.1" // todo move to buildSrc... processor plugin?
}

dependencies {
  ksp(projects.stoikExposedProcessor)
  implementation(projects.stoikExposedCore)

  val exposedVersion = "0.36.2"
  implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
  implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
  implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
}

kotlin {
  sourceSets.main {
    kotlin.srcDir("build/generated/ksp/main/kotlin")
  }
  sourceSets.test {
    kotlin.srcDir("build/generated/ksp/test/kotlin")
  }
}
