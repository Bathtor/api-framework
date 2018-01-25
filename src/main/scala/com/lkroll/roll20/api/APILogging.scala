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

trait APILogging {

  protected var debugOn: Boolean = true;

  def error(s: String, observeNewlines: Boolean = false): Unit = {
    log(s"ERROR: ${s}", observeNewlines);
  }

  def error(t: Throwable): Unit = error(t, false);
  def error(t: Throwable, observeNewlines: Boolean): Unit = {
    error(t.toString, observeNewlines);
    debug(t, observeNewlines);
  }

  def warn(s: String, observeNewlines: Boolean = false): Unit = {
    log(s"WARNING: ${s}", observeNewlines);
  }

  def info(s: String, observeNewlines: Boolean = false): Unit = {
    log(s"INFO: ${s}", observeNewlines);
  }

  private def debug(t: Throwable, observeNewlines: Boolean): Unit = {
    if (debugOn) {
      val stringWriter = new java.io.StringWriter();
      val printWriter = new java.io.PrintWriter(stringWriter);
      t.printStackTrace(printWriter);
      printWriter.flush();
      printWriter.close();
      log(stringWriter.toString(), observeNewlines);
    }
  }

  def debug(s: String, observeNewlines: Boolean = false): Unit = {
    if (debugOn) {
      log(s"DEBUG: ${s}", observeNewlines);
    }
  }

  def log(s: String, observeNewlines: Boolean = false): Unit = {
    if (observeNewlines) {
      val lines = s.split("\n");
      lines.foreach(Roll20API.log(_));
    } else {
      Roll20API.log(s);
    }
  }
}
