plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  id("io.bkbn.sourdough.application.jvm")
  id("org.jetbrains.kotlinx.kover")
  id("com.google.devtools.ksp")
  application
}

sourdoughApp {
  compilerArgs.set(listOf("-opt-in=kotlin.RequiresOptIn"))
}

dependencies {
  // Versions
  val kotlinxDatetimeVersion: String by project
  val kotlinxSerializationVersion: String by project
  val komapperVersion: String by project

  // Lerasium
  ksp(projects.lerasiumCoreProcessor)
  implementation(projects.lerasiumCore)

  ksp(projects.lerasiumRdbmsProcessor)
  implementation(projects.lerasiumRdbms)

  ksp(projects.lerasiumApiProcessor)
  implementation(projects.lerasiumApi)

  ksp(projects.lerasiumMongoProcessor)
  implementation(projects.lerasiumMongo)

  ksp(projects.lerasiumPermissionsProcessor)
  implementation(projects.lerasiumPermissions)

  // Komapper
  ksp(platform("org.komapper:komapper-platform:$komapperVersion"))
  ksp("org.komapper:komapper-processor")

  // Date
  implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDatetimeVersion")

  // Serialization
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")

  // Logging (TODO Replace w/ Kermit!)
  implementation("org.apache.logging.log4j:log4j-api-kotlin:1.2.0")
  implementation("org.apache.logging.log4j:log4j-api:2.19.0")
  implementation("org.apache.logging.log4j:log4j-core:2.19.0")
  implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.19.0")
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
  mainClassName = "io.bkbn.lerasium.playground.MainKt"
}
