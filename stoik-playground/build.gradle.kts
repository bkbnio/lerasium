plugins {
  id("kotlin-application-conventions")
  id("com.google.devtools.ksp") version "1.5.31-1.0.1"
}

kotlin {
  sourceSets.main {
    kotlin.srcDir("build/generated/ksp/main/kotlin")
  }
  sourceSets.test {
    kotlin.srcDir("build/generated/ksp/test/kotlin")
  }
}
