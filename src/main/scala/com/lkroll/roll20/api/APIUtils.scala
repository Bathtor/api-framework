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

import scalajs.js
import scalajs.js.JSON
import com.lkroll.roll20.api.facade.{ Roll20API, Roll20Extras };
import com.lkroll.roll20.core._
import concurrent.{ Future, Promise, ExecutionContext }
import util.{ Try, Success, Failure }

trait APIUtils {

  /*
   *  Required
   */
  implicit def ec: ExecutionContext;
  def outputTemplate: Option[TemplateRef];

  /*
   * Provided
   */
  def extractSimpleRowId(id: String): String = id.split('_').last;

  /**
   * Generate a new repeating section rowId with the same code as the sheetworkers use.
   *
   * If generating multiple rowIds in a row, make sure to double check for duplicates, as generation is time seeded.
   *
   * Alternatively use [[generateRowIds]] or [[RowIdPool]] to manage duplicate avoidance for you.
   */
  def generateRowId(): String = Roll20Extras.generateRowID();
  /**
   * Generate `n` new repeating section rowIDs with the same code as the sheetworkers use.
   *
   * This implementation avoids generating duplicates,
   * but it will block until it has the full number of IDs available,
   * potentially wasting processing time.
   *
   * Alternatively use [[RowIdPool]] to manage duplicate avoidance for you.
   */
  def generateRowIds(n: Int): List[String] = {
    var builder = Set.empty[String];
    while (builder.size < n) {
      builder += generateRowId();
    }
    builder.toList
  }

  def sendChat(speakingAs: String, input: APIChatOutMessage): Unit = {
    sendChat(speakingAs, None, input)
  }
  def sendChat(speakingAs: String, title: String, input: APIChatOutMessage): Unit = {
    sendChat(speakingAs, Some(title), input)
  }

  def sendChat(speakingAs: String, title: Option[String], input: APIChatOutMessage): Unit = {
    val titleS = title.getOrElse("API Message");
    val tMsg = this.msgToTemplate(titleS, input);
    Roll20API.sendChat(speakingAs, tMsg.render);
  }
  def sendChatWarning(speakingAs: String, input: APIChatOutMessage): Unit = {
    val tMsg = this.msgToTemplate("API Warning", input, isWarning = true);
    Roll20API.sendChat(speakingAs, tMsg.render);
  }
  def sendChatError(speakingAs: String, input: APIChatOutMessage): Unit = {
    val tMsg = this.msgToTemplate("API Error", input, isError = true);
    Roll20API.sendChat(speakingAs, tMsg.render);
  }

  def sendChat(speakingAs: PlayerInfo, input: APIChatOutMessage): Unit = {
    val sas = s"player|${speakingAs.id}";
    sendChat(speakingAs, None, input)
  }
  def sendChat(speakingAs: PlayerInfo, title: String, input: APIChatOutMessage): Unit = {
    sendChat(speakingAs, Some(title), input)
  }
  def sendChat(speakingAs: PlayerInfo, title: Option[String] = None, input: APIChatOutMessage): Unit = {
    val sas = s"player|${speakingAs.id}";
    sendChat(sas, title, input)
  }

  def sendChatHeader(speakingAs: String, title: String, input: APIChatOutMessage): Unit = {
    val tMsg = this.msgToTemplate(title, input, showHeader = true, showFooter = false);
    Roll20API.sendChat(speakingAs, tMsg.render);
  }

  def sendChatBody(speakingAs: String, input: APIChatOutMessage): Unit = {
    val tMsg = this.msgToTemplate("UNUSED", input, showHeader = false, showFooter = false);
    Roll20API.sendChat(speakingAs, tMsg.render);
  }

  def sendChatFooter(speakingAs: String, input: APIChatOutMessage): Unit = {
    val tMsg = this.msgToTemplate("UNUSED", input, showHeader = false, showFooter = true);
    Roll20API.sendChat(speakingAs, tMsg.render);
  }

  /*
   * Chat Rolls
   */
  def rollViaChat[T](roll: Rolls.SimpleRoll[T])(implicit reader: Readable[T]): Future[T] = {
    val p = Promise[String]();
    Roll20API.sendChat("API Framework", roll.render, extractRollSimple(_, p));
    p.future.transform(readTransformer(reader))
  }
  def rollViaChat[T](roll: Rolls.InlineRoll[T])(implicit reader: Readable[T]): Future[T] = {
    val p = Promise[String]();
    Roll20API.sendChat("API Framework", roll.render, extractRollInline(_, p));
    p.future.transform(readTransformer(reader))
  }

  private def msgToTemplate(
    title:      String,
    input:      APIChatOutMessage,
    showHeader: Boolean           = true,
    showFooter: Boolean           = true,
    isWarning:  Boolean           = false,
    isError:    Boolean           = false): ChatOutMessage = {
    import APIChatOutMessage._;

    val res = input match {
      case CoreMessage(SimpleMessage(c))       => msgStringToTemplate(title, c, Chat.Default, showHeader, showFooter, isWarning, isError)
      case CoreMessage(CommandMessage(cmd, c)) => msgStringToTemplate(title, c, cmd, showHeader, showFooter, isWarning, isError)
      case t: TemplateMessage                  => t.asCore
    };

    Roll20API.log(s"About to send: ${res.render}");
    res
  }
  private def msgStringToTemplate(
    titleText:   String,
    contentText: String,
    cmd:         ChatCommand,
    showHeader:  Boolean     = true,
    showFooter:  Boolean     = true,
    isWarning:   Boolean     = false,
    isError:     Boolean     = false): ChatOutMessage = {
    import scalatags.Text.all._;
    import com.lkroll.roll20.api.templates._;
    import TemplateImplicits._;

    require(!(isWarning && isError), "A message can only be either a warning or an error, not both!");

    val res = outputTemplate match {
      case Some(ot) => {
        import ModelExtraImplicits._;

        val appl = ot.fillWith(
          ModelOutputTemplate.Fields.showHeader <<= showHeader,
          ModelOutputTemplate.Fields.showFooter <<= showHeader,
          ModelOutputTemplate.Fields.isWarning <<= isWarning,
          ModelOutputTemplate.Fields.isError <<= isError,
          ModelOutputTemplate.Fields.titleField <<= titleText,
          ModelOutputTemplate.Fields.contentField <<= contentText);
        cmd.message(appl.render)
      }
      case None => {
        val msg = frag(h2(titleText), p(contentText));
        cmd.message(msg.render)
      }
    };
    res
  }

  private def readTransformer[T](reader: Readable[T]): Try[String] => Try[T] = {
    case Success(s) => Try(reader.read(s).get)
    case Failure(e) => Failure(e)
  }

  private def extractRollSimple[T](replies: js.Array[Roll20API.ChatMessage], promise: Promise[String]): Unit = {
    try {
      val reply = ChatContext.fromMsg(replies.head);
      //APILogger.debug(s"Roll20 Roll: ${reply.toDetailedString()}");
      assert(reply.`type` == ChatType.rollresult);
      val result = JSON.parse(reply.raw.content).asInstanceOf[Roll20API.InlineRollResults];
      promise.success(result.total.toString());
    } catch {
      case e: Throwable => promise.failure(e)
    }
  }

  private def extractRollInline[T](replies: js.Array[Roll20API.ChatMessage], promise: Promise[String]): Unit = {
    try {
      val reply = ChatContext.fromMsg(replies.head);
      //APILogger.debug(s"Roll20 Roll: ${reply.toDetailedString()}");
      assert(reply.`type` == ChatType.general);
      val result = reply.inlineRolls.head.results.total;
      promise.success(result);
    } catch {
      case e: Throwable => promise.failure(e)
    }
  }
}

object APIUtils extends APIUtils {
  implicit val ec: ExecutionContext = scala.scalajs.concurrent.JSExecutionContext.runNow;
  override def outputTemplate: Option[TemplateRef] = None;
}

case class ContextAPIUtils(context: ChatContext) extends APIUtils {
  implicit val ec: ExecutionContext = scala.scalajs.concurrent.JSExecutionContext.runNow;
  override def outputTemplate: Option[TemplateRef] = context.outputTemplate;
}

/**
 * Maintains the last generated rowId to make sure that [[RowIdPool.generateRowId]] never emits duplicates.
 */
class RowIdPool {
  private var lastId: Option[String] = None;

  /**
   * Generate a new repeating section rowId with the same code as the sheetworkers use.
   *
   * This method guarantees that no duplicates will be generated, at the cost of potentially hot-blocking
   * the execution if fresh ids are requested more rapidly than can be accommodated.
   */
  def generateRowId(): String = {
    while (true) {
      val id = Roll20Extras.generateRowID();
      lastId match {
        case Some(lid) if lid == id => // keep going
        case Some(lid) if lid != id => lastId = Some(id); return id;
        case None                   => lastId = Some(id); return id;
      }
    }
    ??? // dead code
  }
}

object RowIdPool {
  def apply(): RowIdPool = new RowIdPool();
}
