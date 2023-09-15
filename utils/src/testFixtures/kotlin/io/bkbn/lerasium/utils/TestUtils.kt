package io.bkbn.lerasium.utils

import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.intellij.lang.annotations.Language
import java.io.File

object TestUtils {
  // ⚠️ This should only be flipped on if you are trying to update the snapshots
  private const val OVERWRITE_SNAPSHOTS = false

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

  private fun getFileSnapshot(fileName: String): String {
    val snapshotPath = "src/test/resources"
    val file = File("$snapshotPath/$fileName")
    return file.readTrimmed()
  }

  private fun KotlinCompilation.Result.getFileContents(fileName: String): String {
    val file = kspGeneratedSources.find { it.name == fileName }
    return file?.readTrimmed() ?: error("Unable to find file: $fileName")
  }

  fun verifyGeneratedCode(
    source: String,
    provider: SymbolProcessorProvider,
    expectedFileCount: Int,
    fileUnderTest: String,
    fileSnapshot: String
  ) {
    verifyGeneratedCode(
      source = SourceFile.kotlin("Spec.kt", getFileSnapshot(source)),
      provider = provider,
      expectedFileCount = expectedFileCount,
      fileUnderTest = fileUnderTest,
      fileSnapshot = fileSnapshot
    )
  }

  fun verifyGeneratedCode(
    source: SourceFile,
    provider: SymbolProcessorProvider,
    expectedFileCount: Int,
    fileUnderTest: String,
    fileSnapshot: String
  ) {
    // arrange
    val compilation = KotlinCompilation().apply {
      sources = listOf(source)
      symbolProcessorProviders = listOf(provider)
      inheritClassPath = true
    }

    // act
    val result = compilation.compile()
    if (OVERWRITE_SNAPSHOTS) overwriteSnapshot(result.getFileContents(fileUnderTest), fileSnapshot)

    // assert
    result shouldNotBe null
    result.kspGeneratedSources shouldHaveSize expectedFileCount
    result.getFileContents(fileUnderTest) shouldBe getFileSnapshot(fileSnapshot)
  }

  fun verifyGeneratedCode(
    source: String,
    provider: SymbolProcessorProvider,
    expectedFileCount: Int,
    filesUnderTest: Map<String, String>
  ) {
    verifyGeneratedCode(
      source = SourceFile.kotlin("Spec.kt", getFileSnapshot(source)),
      provider = provider,
      expectedFileCount = expectedFileCount,
      filesUnderTest = filesUnderTest
    )
  }


  fun verifyGeneratedCode(
    source: SourceFile,
    provider: SymbolProcessorProvider,
    expectedFileCount: Int,
    filesUnderTest: Map<String, String>
  ) {
    // arrange
    val compilation = KotlinCompilation().apply {
      sources = listOf(source)
      symbolProcessorProviders = listOf(provider)
      inheritClassPath = true
    }

    // act
    val result = compilation.compile()
    if (OVERWRITE_SNAPSHOTS) filesUnderTest.forEach { (fut, fs) -> overwriteSnapshot(result.getFileContents(fut), fs) }

    // assert
    result shouldNotBe null
    result.kspGeneratedSources shouldHaveSize expectedFileCount
    filesUnderTest.forEach { (fileUnderTest, fileSnapshot) ->
      withClue("Failed for file: $fileUnderTest") {
        result.getFileContents(fileUnderTest) shouldBe getFileSnapshot(fileSnapshot)
      }
    }
  }

  private fun overwriteSnapshot(fileUnderTest: String, fileSnapshot: String) {
    val snapshotPath = "src/test/resources"
    val file = File("$snapshotPath/$fileSnapshot")
    file.writeText(fileUnderTest)
  }
}
