package io.bkbn.lerasium.utils

import com.tschuchort.compiletesting.KotlinCompilation
import org.intellij.lang.annotations.Language
import java.io.File

object TestUtils {
  const val errorMessage = "\"\"Unable to get entity with id: \$id\"\""

  private val KotlinCompilation.Result.workingDir: File
    get() =
      outputDirectory.parentFile!!

  val KotlinCompilation.Result.kspGeneratedSources: List<File>
    get() {
      val kspWorkingDir = workingDir.resolve("ksp")
      val kspGeneratedDir = kspWorkingDir.resolve("sources")
      val kotlinGeneratedDir = kspGeneratedDir.resolve("kotlin")
      return kotlinGeneratedDir.walkTopDown().toList().filter { it.isFile }
    }

  fun File.readTrimmed() = readText().trim()

  fun kotlinCode(
    @Language("kotlin") contents: String,
    postProcess: (String) -> String = { it }
  ): String = postProcess(contents)
}
