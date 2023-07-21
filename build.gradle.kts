import io.bkbn.sourdough.gradle.library.jvm.LibraryJvmPlugin
import io.bkbn.sourdough.gradle.library.jvm.LibraryJvmExtension

plugins {
  kotlin("jvm") version "1.9.0" apply false
  kotlin("plugin.serialization") version "1.9.0" apply false
  id("io.bkbn.sourdough.library.jvm") version "0.12.0" apply false
  id("io.bkbn.sourdough.application.jvm") version "0.12.0" apply false
  id("com.google.devtools.ksp") version "1.9.0-1.0.12" apply false
  id("io.bkbn.sourdough.root") version "0.12.0"
  id("com.github.jakemarsden.git-hooks") version "0.0.2"
  id("org.jetbrains.kotlinx.kover") version "0.6.1"
  id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
}

gitHooks {
  setHooks(
    mapOf(
      "pre-commit" to "detekt",
      "pre-push" to "test"
    )
  )
}

allprojects {
  group = "io.bkbn"
  version = run {
    val baseVersion =
      project.findProperty("project.version") ?: error("project.version needs to be set in gradle.properties")
    when ((project.findProperty("release") as? String)?.toBoolean()) {
      true -> baseVersion
      else -> "$baseVersion-SNAPSHOT"
    }
  }
}

subprojects {
  plugins.withType(LibraryJvmPlugin::class.java) {
    extensions.configure(LibraryJvmExtension::class.java) {
      githubOrg.set("bkbnio")
      githubRepo.set("lerasium")
      licenseName.set("MIT License")
      licenseUrl.set("https://mit-license.org")
      developerId.set("unredundant")
      developerName.set("Ryan Brink")
      developerEmail.set("admin@bkbn.io")
    }
  }
}
