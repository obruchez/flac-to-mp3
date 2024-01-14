# flac-to-mp3

[![Scala CI](https://github.com/obruchez/flac-to-mp3/actions/workflows/scala.yml/badge.svg)](https://github.com/obruchez/flac-to-mp3/actions/workflows/scala.yml)

Convert a whole directory structure from FLAC (or other audio formats) to MP3 (or AAC), using ffmpeg. Metadata tags are preserved. Non-audio files are copied.

```
Usage: java -jar flac-to-mp3.jar [options] source_directory destination_directory

Options:

-trash trash_directory   directory where removed destination files will be put (default is destination_directory/.FlacToMp3Trash)
-nodelete                do not delete any file in the destination directory
-extensions extensions   comma-separated list of extensions to convert using ffmpeg (default is flac,flv,m4a,mp2,mp3,mpc,ogg,wav)
-format format           output format (aac or mp3)
-cbr bitrate             CBR bitrate (e.g. 128k or 192000)
-vbr quality             VBR quality (1-5 for AAC and 0-9 for MP3)
-volume mode             volume mode (track for track ReplayGain application, album for album ReplayGain application)
-threads count           number of parallel threads to use
-convertedonly           only copy converted files (i.e. do not copy other files)
-copycovers              copy cover art to sub-directories (useful for e.g. Logitech Media Server)
-force                   force convert/copy even if destination file exists and is up-to-date
-noop                    do not convert, copy, or remove any file in the destination directory
```

Current limitations:

* OGG metadata tags are not read/copied correctly by ffmpeg
* "custom" metadata tags (e.g. ReplayGain, MusicBrainz, etc.) cannot be written/copied to a M4A/MP4 files (this is either a bug in ffmpeg or a limitation of the containers)
