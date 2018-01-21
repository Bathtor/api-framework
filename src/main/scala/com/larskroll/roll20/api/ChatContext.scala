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

import com.larskroll.roll20.api.facade.Roll20API.{ InlineRollResults => FacadeIRR, InlineRoll => FacadeIR, _ }
import scalajs.js
import scalajs.js.JSON

object ChatType extends Enumeration {
  type ChatType = Value;

  val general, rollresult, gmrollresult, emote, whisper, desc, api = Value;
}

case class PlayerInfo(id: String, name: String) {
  private lazy val whisperToName = name.split(" ")(0);
  def whisperTo: String = s"/w $whisperToName";
}

case class InlineRollResults(total: String, raw: FacadeIRR)
case class InlineRoll(expression: String, results: InlineRollResults, rollId: Option[String], signature: Option[String])

object InlineRoll {
  def fromRaw(raw: FacadeIR): InlineRoll = {
    val rollid = raw.rollid.toOption;
    val signature = raw.signature.toString() match {
      case "false" => None
      case x       => Some(x)
    };
    val rawResults = raw.results;
    val total = rawResults.total.toString();
    val results = InlineRollResults(total, rawResults);
    InlineRoll(raw.expression, results, rollid, signature)
  }
}

case class ChatContext(player: PlayerInfo, `type`: ChatType.ChatType, raw: ChatMessage) {
  def selected: List[Roll20Object] = if (raw.selected.isEmpty) { List.empty } else { raw.selected.get.toList };
  def target: Option[PlayerInfo] = {
    for {
      target <- raw.target.toOption;
      target_name <- raw.target_name.toOption
    } yield PlayerInfo(target, target_name)
  }
  def rollTemplate: Option[String] = raw.rolltemplate.toOption;

  def inlineRolls: List[InlineRoll] = {
    raw.inlinerolls.toOption match {
      case Some(irs) => {
        irs.map(InlineRoll.fromRaw(_)).toList
      }
      case None => Nil
    }
  }

  def origRoll: Option[String] = raw.origRoll.toOption;

  def reply(msg: String): Unit = {
    sendChat("API Framework", s"${player.whisperTo} <p>$msg</p>");
  }

  def reply(sender: String, msg: String): Unit = {
    sendChat(sender, s"${player.whisperTo} <p>$msg</p>");
  }

  def toDetailedString(): String = {
    val sb = new StringBuilder();
    var indent = "  ";
    sb ++= s"ChatContext{ type=${`type`}\n";
    sb ++= s"${indent}from = $player\n";
    sb ++= s"${indent}content = ${raw.content}\n";
    sb ++= s"${indent}origRoll = ${opt2String(origRoll)}\n";
    val irs = inlineRolls;
    if (irs.isEmpty) {
      sb ++= s"${indent}inlinerolls = Nil\n"
    } else {
      sb ++= s"${indent}inlinerolls: {\n";
      indent = "    ";
      irs.foreach { ir =>
        sb ++= s"${indent}$ir\n";
      }
      indent = "  ";
      sb ++= s"${indent}}\n";
    }
    sb ++= s"${indent}rolltemplate = ${opt2String(rollTemplate)}\n";
    sb ++= s"${indent}target = ${opt2String(target)}\n";
    val sels = selected;
    if (sels.isEmpty) {
      sb ++= s"${indent}selected = Nil\n"
    } else {
      sb ++= s"${indent}selected: {\n";
      indent = "    ";
      sels.foreach { sel =>
        sb ++= s"${indent}${JSON.stringify(sel)}\n";
      }
      indent = "  ";
      sb ++= s"${indent}}\n";
    }
    sb ++= "}";
    sb.result()
  }

  private def opt2String[T](ot: Option[T]): String = ot match {
    case Some(t) => t.toString()
    case None    => "None"
  }

  override def toString(): String = {
    s"ChatContext($player, type=${`type`}) from ${JSON.stringify(raw)}"
  }
}

object ChatContext {
  def fromMsg(msg: ChatMessage): ChatContext = ChatContext(PlayerInfo(msg.playerid, msg.who), ChatType.withName(msg.`type`), msg);
}
