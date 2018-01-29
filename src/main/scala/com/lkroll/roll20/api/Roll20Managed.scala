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

import com.lkroll.roll20.api.facade.Roll20API._;

object Roll20Managed {
  object Properties {
    val id = "_id";
    val `type` = "_type";
  }
}

trait Roll20Managed {
  import Roll20Managed._;

  /**
   * The underlying facade object.
   */
  def raw: Roll20Object;

  /**
   * Removed the object from Roll20.
   *
   * Using the object afterwards is unspecified behaviour!
   *
   */
  def remove(): Unit = raw.remove();

  /**
   * A unique ID for this object.
   *
   * Globally unique across all objects in this game. Read-only.
   */
  def id: String = raw.get(Properties.id).asInstanceOf[String];

  /**
   * Can be used to identify the object type or search for the object. Read-only.
   */
  def `type`: String = raw.get(Properties.`type`).asInstanceOf[String];
}
