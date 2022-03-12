package org.bruchez.olivier.flactomp3

import java.nio.file.{Files, Path}

object CoverArt {
  def coverArt(path: Path): Boolean =
    Files.isRegularFile(path) && filenames.contains(path.getFileName.toString)

  def expectedCoverArtSubLocations(path: Path)(implicit arguments: Arguments): Seq[Path] =
    if (arguments.copyCoversToSubDirectories) {
      for {
        subDirectory <- FileUtils
          .allFilesInPath(path.getParent, recursive = false)
          .filter(Files.isDirectory(_))
        candidateFile = subDirectory.resolve(path.getFileName)
        if !Files.exists(candidateFile)
        if directoryContainsAudioFiles(subDirectory)
      } yield candidateFile
    } else {
      // Cover art not copied to sub-directories => no expected extra locations
      Seq()
    }

  private def directoryContainsAudioFiles(directory: Path)(implicit arguments: Arguments): Boolean =
    FileUtils
      .allFilesInPath(directory, recursive = false)
      .filter(Files.isRegularFile(_))
      .exists(file =>
        arguments.inputExtensionsToConvert.exists(extension => file.toString.endsWith(extension))
      )

  private val filenames = Set(
    "cover.jpg",
    "cover.gif",
    "folder.jpg",
    "folder.gif",
    "album.jpg",
    "album.gif",
    "thumb.jpg",
    "thumb.gif",
    "albumartsmall.jpg",
    "albumartsmall.gif"
  )
}
