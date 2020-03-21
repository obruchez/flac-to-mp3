package org.bruchez.olivier.flactomp3

sealed trait Volume {
  def ffmpegArguments: Seq[String]
}

case object NoVolumeChange extends Volume {
  override val ffmpegArguments: Seq[String] = Seq()
}

case object ApplyTrackReplayGain extends Volume {
  override val ffmpegArguments: Seq[String] = Seq("-af", "volume=replaygain=track")
}

case object ApplyAlbumReplayGain extends Volume {
  override val ffmpegArguments: Seq[String] = Seq("-af", "volume=replaygain=album")
}
