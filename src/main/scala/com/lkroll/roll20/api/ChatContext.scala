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

import com.lkroll.roll20.api.facade.Roll20API.{
  InlineRoll => FacadeIR,
  InlineRollResults => FacadeIRR,
  _
}
import com.lkroll.roll20.core.{TemplateApplication => _, _}
import scalajs.js.JSON
import scalatags.Text.all._

case class InlineRollResults(total: String, raw: FacadeIRR)
case class InlineRoll(
    expression: String,
    results: InlineRollResults,
    rollId: Option[String],
    signature: Option[String])

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

class ChatContext(
    val player: PlayerInfo,
    val `type`: ChatType.ChatType,
    val raw: ChatMessage,
    val outputTemplate: Option[TemplateRef]) {

  import APIImplicits._;

  private lazy val utils: APIUtils = ContextAPIUtils(this);

  def selected: List[Graphic] = {
    val objs = if (raw.selected.isEmpty) {
      List.empty
    } else {
      raw.selected.get.toList
    };
    objs.flatMap(sel => Graphic.get(sel._id))
  }
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

  def reply(tapp: TemplateApplication): Unit = {
    utils.sendChat("API Framework", player.whisperTo.templateMessage(tapp));
  }
  def reply(msg: String): Unit = reply(p(msg));
  def replyWarn(msg: String): Unit = replyWarn(p(msg));
  def replyError(msg: String): Unit = replyError(p(msg));
  def reply(title: String, msg: String): Unit = reply(title, p(msg));
  def replyHeader(title: String, msg: String): Unit = replyHeader(title, p(msg));
  def replyBody(msg: String): Unit = replyBody(p(msg));
  def replyFooter(msg: String): Unit = replyFooter(p(msg));

  def reply(msg: Tag): Unit = {
    utils.sendChat("API Framework", player.whisperTo.message(msg.render));
  }
  def replyWarn(msg: Tag): Unit = {
    utils.sendChatWarning("API Framework", player.whisperTo.message(msg.render));
  }
  def replyError(msg: Tag): Unit = {
    utils.sendChatError("API Framework", player.whisperTo.message(msg.render));
  }
  def reply(title: String, msg: Tag): Unit = {
    utils.sendChat("API Framework", title, player.whisperTo.message(msg.render));
  }
  def replyHeader(title: String, msg: Tag): Unit = {
    utils.sendChatHeader("API Framework", title, player.whisperTo.message(msg.render));
  }
  def replyBody(msg: Tag): Unit = {
    utils.sendChatBody("API Framework", player.whisperTo.message(msg.render));
  }
  def replyFooter(msg: Tag): Unit = {
    utils.sendChatFooter("API Framework", player.whisperTo.message(msg.render));
  }

  def replyAs(sender: String, tapp: TemplateApplication): Unit = {
    utils.sendChat(sender, player.whisperTo.templateMessage(tapp));
  }
  def replyAs(sender: String, msg: String): Unit = {
    utils.sendChat(sender, player.whisperTo.message(s"<p>$msg</p>"));
  }
  def replyAs(sender: String, msg: Tag): Unit = {
    utils.sendChat(sender, player.whisperTo.message(msg.render));
  }
  def replyAs(sender: String, title: String, msg: String): Unit = {
    utils.sendChat(sender, title, player.whisperTo.message(s"<p>$msg</p>"));
  }
  def replyAs(sender: String, title: String, msg: Tag): Unit = {
    utils.sendChat(sender, title, player.whisperTo.message(msg.render));
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
        sb ++= s"${indent}$sel\n";
      }
      indent = "  ";
      sb ++= s"${indent}}\n";
    }
    sb ++= "}";
    sb.result()
  }

  private def opt2String[T](ot: Option[T]): String =
    ot match {
      case Some(t) => t.toString()
      case None    => "None"
    }

  override def toString(): String = {
    s"ChatContext($player, type=${`type`}) from ${JSON.stringify(raw)}"
  }
}

object ChatContext {
  def fromMsg(msg: ChatMessage): ChatContext =
    new ChatContext(PlayerInfo(msg.playerid, msg.who), ChatType.withName(msg.`type`), msg, None);
  def fromMsg(msg: ChatMessage, outputTemplate: TemplateRef): ChatContext =
    new ChatContext(
      PlayerInfo(msg.playerid, msg.who),
      ChatType.withName(msg.`type`),
      msg,
      Some(outputTemplate));
  def fromMsg(msg: ChatMessage, outputTemplate: Option[TemplateRef]): ChatContext =
    new ChatContext(
      PlayerInfo(msg.playerid, msg.who),
      ChatType.withName(msg.`type`),
      msg,
      outputTemplate);
}
