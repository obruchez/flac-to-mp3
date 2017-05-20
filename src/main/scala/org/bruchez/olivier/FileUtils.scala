package org.bruchez.olivier

import java.nio.file._

import org.apache.commons.io.FilenameUtils

object FileUtils {
  def baseNameAndExtension(path: Path): (String, Option[String]) =
    (FilenameUtils.getBaseName(path.toString), Some(FilenameUtils.getExtension(path.toString)).filterNot(_.isEmpty))

  def dumpExtensionStatistics(path: Path): Unit = {
    import collection.JavaConverters._

    val counts = collection.mutable.Map[String, Int]()

    for {
      path <- Files.walk(path).iterator().asScala
      (_, extensionOption) = baseNameAndExtension(path)
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

  def macOsMetadataFile(path: Path): Boolean =
    path.toString == MacOsDsStoreFilename || path.toString.startsWith(MacOsMetadataFilePrefix)

  private val MacOsDsStoreFilename = ".DS_Store"
  private val MacOsMetadataFilePrefix = "._"

  def osMetadataFile(path: Path): Boolean =
    macOsMetadataFile(path) || windowsMetadataFile(path)

  def windowsMetadataFile(path: Path): Boolean =
    WindowsThumbsDbFilename.contains(path.toString)

  private val WindowsThumbsDbFilename = Seq("Thumbs.db", "desktop.ini", "Desktop.ini")
}
