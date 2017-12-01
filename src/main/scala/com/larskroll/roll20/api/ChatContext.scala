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

import com.larskroll.roll20.api.facade.Roll20API._
import scalajs.js

object ChatType extends Enumeration {
  type ChatType = Value;

  val general, rollresult, gmrollresult, emote, whisper, desc, api = Value;
}

case class PlayerInfo(id: String, name: String) {
  private lazy val whisperToName = name.split(" ")(0);
  def whisperTo: String = s"/w $whisperToName";
}

case class ChatContext(player: PlayerInfo, `type`: ChatType.ChatType, raw: ChatMessage) {
  def selected: Seq[Roll20Object] = if (raw.selected.isEmpty) { Seq.empty } else { raw.selected.get.toSeq };
  def target: Option[PlayerInfo] = {
    for {
      target <- raw.target.toOption;
      target_name <- raw.target_name.toOption
    } yield PlayerInfo(target, target_name)
  }
  def rolltemplate: Option[String] = raw.rolltemplate.toOption;

  def reply(msg: String): Unit = {
    sendChat("API Framework", s"${player.whisperTo} <p>$msg</p>");
  }
  
  def reply(sender: String, msg: String): Unit = {
    sendChat(sender, s"${player.whisperTo} <p>$msg</p>");
  }
}

object ChatContext {
  def fromMsg(msg: ChatMessage): ChatContext = ChatContext(PlayerInfo(msg.playerid, msg.who), ChatType.withName(msg.`type`), msg);
}
