package org.bruchez.olivier

import java.nio.file._

import scala.util._

case class Arguments(srcPath: Path,
                     dstPath: Path,
                     trashPath: Path,
                     inputExtensionsToConvert: Set[String] = Arguments.DefaultInputExtensionsToConvert,
                     outputFormat: Format = Aac,
                     outputBitrate: Bitrate = Aac.defaultBitrate,
                     threadCount: Int = 4,
                     noop: Boolean = false) {
  def formatSpecificFfmpegArguments: Seq[String] = outputFormat.ffmpegArguments(outputBitrate)
}

object Arguments {
  val DefaultInputExtensionsToConvert = Set("flac", "flv", "m4a", "mp2", "mp3", "mpc", "ogg", "wav")

  def apply(args: Array[String]): Try[Arguments] = {
    if (args.length >= 2) {
      val srcPath = Paths.get(args(args.length - 2)).toAbsolutePath
      val dstPath = Paths.get(args(args.length - 1)).toAbsolutePath
      val trashPath = dstPath.resolve(".FlacToMp3Trash")

      val defaultArguments = Arguments(srcPath = srcPath, dstPath = dstPath, trashPath = trashPath)

      val argumentsTry = fromArgs(args = args.slice(0, args.length - 2).toList, arguments = defaultArguments)

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

  // scalastyle:off cyclomatic.complexity
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
            Success((arguments.copy(outputBitrate = Cbr(remainingArgs.head)), remainingArgs.tail))
          case VariableBitrateArgument if remainingArgs.nonEmpty =>
            Try(remainingArgs.head.toInt) match {
              case Success(quality) =>
                Success((arguments.copy(outputBitrate = Vbr(quality)), remainingArgs.tail))
              case Failure(_) =>
                Failure(new IllegalArgumentException(s"Unexpected quality: ${remainingArgs.head}"))
            }
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
  // scalastyle:on cyclomatic.complexity

  private val TrashPathArgument = "-trash"
  private val InputExtensionsToConvertArgument = "-ext"
  private val OutputFormatArgument = "-f"
  private val ConstantBitrateArgument = "-cbr"
  private val VariableBitrateArgument = "-vbr"
  private val NoopArgument = "-noop"
}
