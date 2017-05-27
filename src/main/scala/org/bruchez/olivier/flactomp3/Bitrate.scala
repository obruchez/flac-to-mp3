package org.bruchez.olivier.flactomp3

sealed trait Bitrate
case class Cbr(targetBitrate: String) extends Bitrate
case class Vbr(quality: Int) extends Bitrate
