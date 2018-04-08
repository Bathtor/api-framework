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

  implicit def ec: ExecutionContext;

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
