plugins {
  id("kotlin-application-conventions")
  id("com.google.devtools.ksp") version "1.6.0-1.0.1" // todo move to buildSrc... processor plugin?
}

dependencies {
  ksp(projects.stoikExposedProcessor)
  implementation(projects.stoikExposedCore)

  ksp(projects.stoikKtorProcessor)
  implementation(projects.stoikKtorCore)
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
  mainClassName = "io.bkbn.stoik.playground.MainKt"
}
