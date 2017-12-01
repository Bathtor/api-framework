/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Lars Kroll <bathtor@googlemail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 */
package com.larskroll.roll20.api

import org.rogach.scallop._
import com.larskroll.roll20.api.facade.Roll20API

object ScallopAPIException {
  def apply(message: String, replyWith: String): ScallopAPIException = new ScallopAPIException(message, Some(replyWith));
  def apply(message: String, replyWith: String, cause: Throwable): ScallopAPIException = new ScallopAPIException(message, replyWith, cause);

  def unapply(e: ScallopAPIException): Option[(String, Option[String], Throwable)] = Some((e.getMessage, e.replyWith, e.getCause))
}

class ScallopAPIException(message: String, val replyWith: Option[String] = None) extends Exception(message) {

  def this(message: String, cause: Throwable) {
    this(message);
    initCause(cause);
  }

  def this(message: String, replyWith: String, cause: Throwable) {
    this(message, Some(replyWith));
    initCause(cause);
  }

  def this(cause: Throwable) {
    this(Option(cause).map(_.toString).orNull, cause);
  }

  def this() {
    this(null: String)
  }
}

abstract class ScallopAPIConf(args: Seq[String] = Nil) extends ScallopConfBase(args) {

  import org.rogach.scallop.exceptions._;

  override protected def guessOptionNameDefault: Boolean = false
  override protected def performOptionNameGuessing() {}

  def helpString(): String = helpString(builder);

  private def helpString(bldr: Scallop): String = {
    val sb = new StringBuilder();

    bldr.vers foreach { v => sb.append("<p>"); sb.append(v.replaceAll("\n", "<br />")); sb.append("</p>"); }
    bldr.bann foreach { b => sb.append("<p>"); sb.append(b.replaceAll("\n", "<br />")); sb.append("</p>"); }
    sb.append(bldr.help.replaceAll("\n", "<br />"));
    bldr.foot foreach { f => sb.append("<p>"); sb.append(f.replaceAll("\n", "<br />")); sb.append("</p>"); }

    sb.toString()
  }

  override def onError(e: Throwable): Unit = e match {
    case Help("")                  => throw ScallopAPIException("User asked for command help.", helpString());
    case Help(subname)             => throw ScallopAPIException("User asked for subcommand help.", helpString(builder.findSubbuilder(subname).get));
    case Version                   => throw ScallopAPIException("User asked for version.", builder.vers.mkString);
    case ScallopException(message) => throw ScallopAPIException("Parsing error: " + message, message);
    case other                     => throw ScallopAPIException("An error occurred during argument parsing.", "An error occurred during command parsing. Consult the API logs for more information.", other);
  }

  //  protected def onError(e: Throwable): Unit = e match {
  //    case r: ScallopResult if !throwError.value => r match {
  //      case Help("") =>
  //        builder.printHelp
  //        sys.exit(0)
  //      case Help(subname) =>
  //        builder.findSubbuilder(subname).get.printHelp
  //        sys.exit(0)
  //      case Version =>
  //        builder.vers.foreach(println)
  //        sys.exit(0)
  //      case ScallopException(message) => errorMessageHandler(message)
  //    }
  //    case e => throw e
  //  }

  errorMessageHandler = { message =>
    Roll20API.log(Util.format("[%s] Error: %s", printedName, message))
    sys.exit(1)
  }

}

trait APICommand[C <: ScallopAPIConf] extends APILogging with APIUtils {
  def command: String;
  def options: Function[Array[String], C];
  def apply(config: C, ctx: ChatContext): Unit;
  def callback: Function2[Array[String], ChatContext, Unit] = (args, ctx) => {
    try {
      val opts = options(args.drop(1));
      apply(opts, ctx);
    } catch {
      case sapie: ScallopAPIException => {
        error(sapie);
        sapie.replyWith.foreach(ctx.reply(_));
      }
    }
  }
}
