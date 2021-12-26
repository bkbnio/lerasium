import io.bkbn.sourdough.gradle.core.extension.SourdoughLibraryExtension

plugins {
  id("io.bkbn.sourdough.root") version "0.2.0"
  id("com.github.jakemarsden.git-hooks") version "0.0.2"
}

gitHooks {
  setHooks(
    mapOf(
      "pre-commit" to "detekt",
      "pre-push" to "test"
    )
  )
}

sourdough {
  toolChainJavaVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_17.majorVersion))
  jvmTarget.set(JavaVersion.VERSION_11.majorVersion)
  compilerArgs.set(listOf("-opt-in=kotlin.RequiresOptIn"))
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
  if (name != "stoik-playground") {
    apply(plugin = "io.bkbn.sourdough.library")

    configure<SourdoughLibraryExtension> {
      githubOrg.set("bkbnio")
      githubRepo.set("stoik")
      libraryName.set("Stoik")
      libraryDescription.set("A different approach to boilerplate generation")
      licenseName.set("MIT License")
      licenseUrl.set("https://mit-license.org")
      developerId.set("bkbnio")
      developerName.set("Ryan Brink")
      developerEmail.set("admin@bkbn.io")
    }
  }
}
