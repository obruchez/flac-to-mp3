package org.bruchez.olivier

import scala.concurrent.Future

case class ActionGroup(actions: Seq[Action])

trait Action {
  def execute(): Future[Unit]
}

/*
 Algorithm idea:
 - get files from source path
 - get files from destination path
 - method to compare both group of files => return ActionGroups
 - Actions can be parallelized only if they're part of the same group, so groups will be processed one after the other
 - this will allow for the following actions to be executed safely: symbolic links removal, file deletion, conversions

File comparison:
 - take source files and generate expected list of destination files (.flac -> .m4a, .mpp -> .m4a, etc.)
 - compare expected destination files and actual destination files => generate actions
 */
