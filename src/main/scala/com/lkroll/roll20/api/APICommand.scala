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
package com.lkroll.roll20.api

import com.lkroll.roll20.api.facade.Roll20API
import concurrent.ExecutionContext

class APIOptionsException(message: String, val replyWith: Option[String] = None) extends Exception(message) {

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

object APIOptionsException {
  def apply(message: String, replyWith: String): APIOptionsException = new APIOptionsException(message, Some(replyWith));
  def apply(message: String, replyWith: String, cause: Throwable): APIOptionsException = new APIOptionsException(message, replyWith, cause);

  def unapply(e: APIOptionsException): Option[(String, Option[String], Throwable)] = Some((e.getMessage, e.replyWith, e.getCause))
}

trait APICommand[C] extends APILogging with APIUtils {

  implicit val ec: ExecutionContext = scala.scalajs.concurrent.JSExecutionContext.runNow;

  def command: String;
  def options: Function[Array[String], C];
  def apply(config: C, ctx: ChatContext): Unit;
  def callback: Function2[Array[String], ChatContext, Unit] = (args, ctx) => {
    try {
      val opts = options(args.drop(1));
      apply(opts, ctx);
    } catch {
      case sapie: APIOptionsException => {
        error(sapie);
        sapie.replyWith.foreach(ctx.reply(_));
      }
    }
  }
}
