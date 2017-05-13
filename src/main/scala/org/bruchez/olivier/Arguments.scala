package org.bruchez.olivier

import java.nio.file._

import scala.util._

case class Arguments(srcPath: Path,
                     dstPath: Path,
                     outputFormat: Format = Aac,
                     outputBitrate: Bitrate = Aac.defaultBitrate)

object Arguments {
  def apply(args: Array[String]): Try[Arguments] = {
    if (args.length >= 2) {
      val defaultArguments = Arguments(
        srcPath = Paths.get(args(args.length - 2)),
        dstPath = Paths.get(args(args.length - 1)))

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

  private val OutputFormatArgument = "-f"
  private val ConstantBitrateArgument = "-cbr"
  private val VariableBitrateArgument = "-vbr"
}
