package org.bruchez.olivier.flactomp3

import java.nio.file._

import org.apache.commons.io.FilenameUtils

import scala.collection.JavaConverters._

object FileUtils {
  def allFilesInPath(path: Path): Seq[Path] =
    Files.walk(path, FileVisitOption.FOLLOW_LINKS).iterator().asScala.toSeq

  def baseNameAndExtension(path: Path): (String, Option[String]) =
    (FilenameUtils.getBaseName(path.toString), Some(FilenameUtils.getExtension(path.toString)).filterNot(_.isEmpty))

  @scala.annotation.tailrec
  def deleteDirectoryAndAllParentDirectoriesIfEmpty(directory: Path): Unit =
    if (emptyDirectory(directory)) {
      Files.delete(directory)
      deleteDirectoryAndAllParentDirectoriesIfEmpty(directory.getParent)
    }

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

  def emptyDirectory(directory: Path): Boolean =
    Files.newDirectoryStream(directory).asScala.toSeq.isEmpty

  def macOsMetadataFile(path: Path): Boolean = {
    val filename = path.getFileName.toString
    filename == MacOsDsStoreFilename || filename.startsWith(MacOsMetadataFilePrefix)
  }

  private val MacOsDsStoreFilename = ".DS_Store"
  private val MacOsMetadataFilePrefix = "._"

  def osMetadataFile(path: Path): Boolean =
    macOsMetadataFile(path) || windowsMetadataFile(path)

  def windowsMetadataFile(path: Path): Boolean =
    WindowsMetadataFilenames.contains(path.getFileName.toString.toLowerCase)

  private val WindowsMetadataFilenames = Seq("desktop.ini", "ehthumbs.db", "thumbs.db")

  def withExtension(path: Path, extension: String): Path =
    Paths.get(FilenameUtils.removeExtension(path.toString) + s".$extension")
}
