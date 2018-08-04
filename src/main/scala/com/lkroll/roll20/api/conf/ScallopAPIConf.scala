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
package com.lkroll.roll20.api.conf

import org.rogach.scallop._
import com.lkroll.roll20.api.APIOptionsException
import com.lkroll.roll20.api.facade.Roll20API

abstract class ScallopAPIConf(args: Seq[String] = Nil) extends ScallopConfBase(args) {

  import org.rogach.scallop.exceptions._;

  override protected def optionNameGuessingSupported: Boolean = false;
  override protected def performOptionNameGuessing() {}

  def helpString(): String = helpString(builder);

  private def helpString(bldr: Scallop): String = {
    bldr.setHelpWidth(Int.MaxValue);

    val sb = new StringBuilder();

    bldr.vers foreach { v => sb.append("<p>"); sb.append(v.replaceAll("\n", "<br />")); sb.append("</p>"); }
    bldr.bann foreach { b => sb.append("<p>"); sb.append(b.replaceAll("\n", "<br />")); sb.append("</p>"); }
    sb.append(bldr.help.replaceAll("\n", "<br />"));
    bldr.foot foreach { f => sb.append("<p>"); sb.append(f.replaceAll("\n", "<br />")); sb.append("</p>"); }

    sb.toString()
  }

  override def onError(e: Throwable): Unit = e match {
    case Help("")                  => throw APIOptionsException("User asked for command help.", helpString());
    case Help(subname)             => throw APIOptionsException("User asked for subcommand help.", helpString(builder.findSubbuilder(subname).get));
    case Version                   => throw APIOptionsException("User asked for version.", builder.vers.mkString);
    case ScallopException(message) => throw APIOptionsException("Parsing error: " + message, message);
    case other                     => throw APIOptionsException("An error occurred during argument parsing.", "An error occurred during command parsing. Consult the API logs for more information.", other);
  }

  errorMessageHandler = { message =>
    Roll20API.log(Util.format("[%s] Error: %s", printedName, message))
    sys.exit(1)
  }

}
