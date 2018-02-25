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
import com.lkroll.roll20.api.facade.Roll20API;
import com.lkroll.roll20.core._
import concurrent.{ Future, Promise, ExecutionContext }
import util.{ Try, Success, Failure }

trait APIUtils {

  implicit def ec: ExecutionContext;

  def extractSimpleRowId(id: String): String = id.split('_').last;

  def sendChat(speakingAs: String, input: ChatOutMessage): Unit = Roll20API.sendChat(speakingAs, input.render);
  def sendChat(speakingAs: PlayerInfo, input: ChatOutMessage): Unit = {
    val sas = s"player|${speakingAs.id}";
    Roll20API.sendChat(sas, input.render);
  }
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
}
