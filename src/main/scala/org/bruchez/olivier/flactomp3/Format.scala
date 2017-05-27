package org.bruchez.olivier.flactomp3

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
  def extension: String
  def ffmpegArguments(bitrate: Bitrate): Seq[String]
  def defaultBitrate: Bitrate
}

case object Aac extends Format {
  private val BaseFfmpegArguments = Seq("-c:a", "libfdk_aac")
  private val DefaultQuality = 4

  override val name: String = "aac"

  override val extension: String = "m4a"

  override def ffmpegArguments(bitrate: Bitrate): Seq[String] = BaseFfmpegArguments ++ (bitrate match {
    case Cbr(targetBitrate) => Seq("-b:a", targetBitrate)
    case Vbr(targetQuality) => Seq("-vbr", targetQuality.toString)
  })

  override val defaultBitrate = Vbr(quality = DefaultQuality)
}

case object Mp3 extends Format {
  private val BaseFfmpegArguments = Seq("-c:a", "libmp3lame", "-id3v2_version", "3")
  private val DefaultQuality = 2

  override val name: String = "mp3"

  override val extension: String = "mp3"

  override def ffmpegArguments(bitrate: Bitrate): Seq[String] = BaseFfmpegArguments ++ (bitrate match {
    case Cbr(targetBitrate) => Seq("-b:a", targetBitrate)
    case Vbr(targetQuality) => Seq("-q:a", targetQuality.toString)
  })

  override val defaultBitrate = Vbr(quality = DefaultQuality)
}
