package org.bruchez.olivier

import scala.util._

object FlacToMp3 {
  def main(args: Array[String]): Unit = {
    Arguments(args) match {
      case Failure(throwable) =>
        System.err.println(throwable.getMessage)
        System.exit(-1)
      case Success(arguments) =>
        println(s"arguments = $arguments")
        // @todo
    }
  }
}
