package org.bruchez.olivier

sealed trait Bitrate
case class Cbr(targetBitrate: String) extends Bitrate
case class Vbr(quality: Int) extends Bitrate
