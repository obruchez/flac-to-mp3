package org.bruchez.olivier

import java.nio.file._

import org.apache.commons.io.FilenameUtils

object FileUtils {
  def baseNameAndExtension(filename: String): (String, Option[String]) =
    (FilenameUtils.getBaseName(filename), Some(FilenameUtils.getExtension(filename)).filterNot(_.isEmpty))

  def dumpExtensionStatistics(path: Path): Unit = {
    import collection.JavaConverters._

    val counts = collection.mutable.Map[String, Int]()

    for {
      path <- Files.walk(path).iterator().asScala
      (_, extensionOption) = baseNameAndExtension(path.toString)
      extension <- extensionOption
      extensionNormalized = extension.trim.toLowerCase
      if extensionNormalized.length < 5
      if scala.util.Try(extensionNormalized.toInt).isFailure
    } {
      counts(extensionNormalized) = counts.getOrElse(extensionNormalized, 0) + 1
    }

    for ((extension, count) <- counts.toSeq.sortBy(_._2).reverse) {
      println(s"$extension: $count")
    }
  }

  def macOsMetadataFile(filename: String): Boolean =
    filename == MacOsDsStoreFilename || filename.startsWith(MacOsMetadataFilePrefix)

  private val MacOsDsStoreFilename = ".DS_Store"
  private val MacOsMetadataFilePrefix = "._"

  def osMetadataFile(filename: String): Boolean =
    macOsMetadataFile(filename) || windowsMetadataFile(filename)

  def windowsMetadataFile(filename: String): Boolean =
    WindowsThumbsDbFilename.contains(filename)

  private val WindowsThumbsDbFilename = Seq("Thumbs.db", "desktop.ini", "Desktop.ini")
}
