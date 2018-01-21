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

import com.larskroll.roll20.api.facade.Roll20API;

sealed trait ChatOutMessage {
  def render: String;
}

final case class SimpleMessage(message: String) extends ChatOutMessage {
  override def render: String = message;
}
final case class ToMessage(to: String, msg: String) extends ChatOutMessage { // FIXME extract Chat types into separate project and reuse
  override def render: String = s"$to $msg";
}
trait APIUtils {

  def extractSimpleRowId(id: String): String = id.split('_').last;

  def sendChat(speakingAs: String, input: ChatOutMessage): Unit = Roll20API.sendChat(speakingAs, input.render);
  def sendChat(speakingAs: PlayerInfo, input: ChatOutMessage): Unit = {
    val sas = s"player|${speakingAs.id}";
    Roll20API.sendChat(sas, input.render);
  }

  implicit def str2ChatMessage(s: String): ChatOutMessage = SimpleMessage(s);
  def to(target: String)(msg: String): ChatOutMessage = ToMessage(target, msg);
}
