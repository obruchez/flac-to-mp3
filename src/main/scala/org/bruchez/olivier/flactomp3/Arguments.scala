package org.bruchez.olivier.flactomp3

import java.nio.file._

import scala.util._

case class Arguments(srcPath: Path,
                     dstPath: Path,
                     trashPath: Path,
                     inputExtensionsToConvert: Set[String] =
                       Arguments.DefaultInputExtensionsToConvert,
                     outputFormat: Format = Aac,
                     outputBitrateOption: Option[Bitrate] = None,
                     threadCount: Int = Math.max(1, Runtime.getRuntime.availableProcessors()),
                     copyCoversToSubDirectories: Boolean = false,
                     force: Boolean = false,
                     noop: Boolean = false) {
  def formatSpecificFfmpegArguments: Seq[String] = outputFormat.ffmpegArguments(outputBitrate)

  def outputBitrate: Bitrate = outputBitrateOption.getOrElse(outputFormat.defaultBitrate)
}

object Arguments {
  val DefaultInputExtensionsToConvert = Set("flac", "flv", "m4a", "mp2", "mp3", "mpc", "ogg", "wav")

  def apply(args: Array[String]): Try[Arguments] = {
    if (args.length >= 2) {
      val srcPath = Paths.get(args(args.length - 2)).toAbsolutePath
      val dstPath = Paths.get(args(args.length - 1)).toAbsolutePath
      val trashPath = dstPath.resolve(".FlacToMp3Trash")

      val defaultArguments = Arguments(srcPath = srcPath, dstPath = dstPath, trashPath = trashPath)

      val argumentsTry =
        fromArgs(args = args.slice(0, args.length - 2).toList, arguments = defaultArguments)

      argumentsTry flatMap { arguments =>
        if (arguments.srcPath.toString == arguments.dstPath.toString) {
          Failure(new IllegalArgumentException("Source and destination paths cannot be the same"))
        } else {
          Success(arguments)
        }
      }
    } else {
      Failure(new IllegalArgumentException("Source and destination paths missing"))
    }
  }

  // scalastyle:off cyclomatic.complexity method.length
  @annotation.tailrec
  private def fromArgs(args: List[String], arguments: Arguments): Try[Arguments] =
    args match {
      case Nil =>
        Success(arguments)
      case arg :: remainingArgs =>
        (arg match {
          case TrashPathArgument if remainingArgs.nonEmpty =>
            Success((arguments.copy(trashPath = Paths.get(remainingArgs.head)), remainingArgs.tail))
          case InputExtensionsToConvertArgument if remainingArgs.nonEmpty =>
            val extensions = remainingArgs.head.split(",").map(_.trim.toLowerCase).toSet
            Success((arguments.copy(inputExtensionsToConvert = extensions), remainingArgs.tail))
          case OutputFormatArgument if remainingArgs.nonEmpty =>
            Format.formatFromString(remainingArgs.head) match {
              case Success(format) =>
                Success((arguments.copy(outputFormat = format), remainingArgs.tail))
              case Failure(_) =>
                Failure(new IllegalArgumentException(s"Unexpected format: ${remainingArgs.head}"))
            }
          case ConstantBitrateArgument if remainingArgs.nonEmpty =>
            Success(
              (arguments.copy(outputBitrateOption = Some(Cbr(remainingArgs.head))),
               remainingArgs.tail))
          case VariableBitrateArgument if remainingArgs.nonEmpty =>
            Try(remainingArgs.head.toInt) match {
              case Success(quality) =>
                Success(
                  (arguments.copy(outputBitrateOption = Some(Vbr(quality))), remainingArgs.tail))
              case Failure(_) =>
                Failure(new IllegalArgumentException(s"Unexpected quality: ${remainingArgs.head}"))
            }
          case ThreadCountArgument if remainingArgs.nonEmpty =>
            Try(remainingArgs.head.toInt) match {
              case Success(count) =>
                Success((arguments.copy(threadCount = count), remainingArgs.tail))
              case Failure(_) =>
                Failure(
                  new IllegalArgumentException(s"Unexpected thread count: ${remainingArgs.head}"))
            }
          case CopyCoversToSubDirectoriesArgument =>
            Success((arguments.copy(copyCoversToSubDirectories = true), remainingArgs))
          case ForceArgument =>
            Success((arguments.copy(force = true), remainingArgs))
          case NoopArgument =>
            Success((arguments.copy(noop = true), remainingArgs))
          case _ =>
            Failure(new IllegalArgumentException(s"Unexpected argument: $arg"))
        }) match {
          case Success((newArguments, argsLeftToParse)) =>
            fromArgs(argsLeftToParse, newArguments)
          case Failure(throwable) =>
            Failure(throwable)
        }
    }
  // scalastyle:on cyclomatic.complexity method.length

  val usage =
    s"""Usage: FlacToMp3 [options] source_directory destination_directory
      |
      |Options:
      |
      |-trash trash_directory   directory where removed destination files will be put (default is destination_directory/.FlacToMp3Trash)
      |-extensions extensions   comma-separated list of extensions to convert using ffmpeg (default is ${DefaultInputExtensionsToConvert.toSeq.sorted
         .mkString(",")})
      |-format format           output format (aac or mp3)
      |-cbr bitrate             CBR bitrate (e.g. 128k or 192000)
      |-vbr quality             VBR quality (1-5 for AAC and 0-9 for MP3)
      |-threads count           number of parallel threads to use
      |-copycovers              copy cover art to sub-directories (useful for e.g. Logitech Media Server)
      |-force                   force convert/copy even if destination file exists and is up-to-date
      |-noop                    do not convert, copy, or remove any file in the destination directory""".stripMargin

  private val TrashPathArgument = "-trash"
  private val InputExtensionsToConvertArgument = "-extensions"
  private val OutputFormatArgument = "-format"
  private val ConstantBitrateArgument = "-cbr"
  private val VariableBitrateArgument = "-vbr"
  private val ThreadCountArgument = "-threads"
  private val CopyCoversToSubDirectoriesArgument = "-copycovers"
  private val ForceArgument = "-force"
  private val NoopArgument = "-noop"
}
