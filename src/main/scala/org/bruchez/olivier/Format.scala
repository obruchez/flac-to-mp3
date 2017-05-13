package org.bruchez.olivier

import scala.util._

object Format {
  val formats = Seq(Aac, Mp3)

  def formatFromString(string: String): Try[Format] = {
    val normalizedString = string.trim.toLowerCase

    formats.find(_.name.trim.toLowerCase == normalizedString) match {
      case Some(format) => Success(format)
      case None => Failure(new IllegalArgumentException(s"Unexpected format: $string"))
    }
  }
}

sealed trait Format {
  def name: String
  def ffmpegArguments(bitrate: Bitrate): Seq[String]
  def defaultBitrate: Bitrate
}

case object Aac extends Format {
  private val baseFfmpegArguments = Seq("-c:a", "libmp3lame")

  override val name: String = "aac"

  override def ffmpegArguments(bitrate: Bitrate): Seq[String] = baseFfmpegArguments ++ (bitrate match {
    case Cbr(targetBitrate) => Seq("-b:a", targetBitrate)
    case Vbr(targetQuality) => Seq("-vbr", targetQuality.toString)
  })

  override val defaultBitrate = Vbr(quality = 5)
}

case object Mp3 extends Format {
  private val baseFfmpegArguments = Seq("-c:a", "libfdk_aac")

  override val name: String = "mp3"

  override def ffmpegArguments(bitrate: Bitrate): Seq[String] = baseFfmpegArguments ++ (bitrate match {
    case Cbr(targetBitrate) => Seq("-b:a", targetBitrate)
    case Vbr(targetQuality) => Seq("-q:a", targetQuality.toString)
  })

  override val defaultBitrate = Vbr(quality = 2)
}
