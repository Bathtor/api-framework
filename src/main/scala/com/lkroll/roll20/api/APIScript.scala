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
import js.annotation._
import com.lkroll.roll20.api.facade.Roll20API
import Roll20API.{ChatMessage, Roll20Object}
// import scala.scalajs.js.Dynamic.{global => dynGlobal, literal => dynLiteral}
// import scala.concurrent.{ExecutionContext, Future, Promise}
import concurrent.ExecutionContext
import collection.mutable

trait APIScriptRoot extends APIScript {
  def children: Seq[APIScript] = Seq.empty;
  def readyChildren: Seq[APIScript] = Seq.empty;

  @JSExport
  def load(): Unit = {
    val allChildren = (children ++ readyChildren);
    subscriptions.foreach {
      case (k, callbacks) => {
        callbacks.foreach { c =>
          debug(s"${this.getClass.getName}: subscribing callback on trigger: ${k}.");
          Roll20API.on(k, c)
        }
      }
    }
    allChildren.foreach(_.internal_load());
  }

  onReady {
    val allChildren = (children ++ readyChildren);
    allChildren.foreach(_.ready = true);
    readyChildren.foreach(_.internal_load());
  }
}

trait APIScript extends APILogging with APIUtils {

  implicit val ec: ExecutionContext = scalajs.concurrent.JSExecutionContext.queue;

  val subscriptions = utils.SubscriptionMap.create;
  val commands = new mutable.HashMap[String, Function2[Array[String], ChatContext, Unit]];
  private var commandsSubscribed: Boolean = false;
  private val commandsSubscription: Function1[ChatMessage, Unit] = (msg) => {
    if (msg.`type` == "api") {
      val chatctx = ChatContext.fromMsg(msg, outputTemplate);
      val args = msg.content.split("\\s+");
      assert(args.length >= 1);
      assert(args(0).startsWith("!"));
      val cmd = args(0).substring(1);
      commands.get(cmd) match {
        case Some(handler) => handler(args, chatctx);
        case None =>
          debug(
            s"${this.getClass.getSimpleName}: No handler found for command ${cmd} in ${args.mkString(" ")}")
      }
    }
  }

  private[api] var ready: Boolean = false;
  def isReady: Boolean = this.ready;

  private[api] def internal_load(): Unit = {
    apiCommands.foreach { c =>
      debug(s"${this.getClass.getName}: registering command ${c.command}");
      onCommand(c.command)(c.callback);
    }
    subscriptions.foreach {
      case (k, callbacks) => {
        callbacks.foreach { c =>
          debug(s"${this.getClass.getName}: subscribing callback on trigger: ${k}.");
          Roll20API.on(k, c)
        }
      }
    }
  }

  def onReady(callback: => Unit): Unit = {
    subscriptions.addBinding("ready", () => callback);
  }

  def onChange(trigger: String, callback: Function2[Roll20Object, Roll20Object, Unit]): Unit = {
    subscriptions.addBinding("change:" + trigger, callback);
  }

  def onAdd(trigger: String, callback: Function1[Roll20Object, Unit]): Unit = {
    subscriptions.addBinding("add:" + trigger, callback);
  }

  def onDestroy(trigger: String, callback: Function1[Roll20Object, Unit]): Unit = {
    subscriptions.addBinding("destroy:" + trigger, callback);
  }

  def onChat(callback: Function1[ChatMessage, Unit]): Unit = {
    subscriptions.addBinding("chat:message", callback);
  }

  def onCommand(command: String)(callback: Function2[Array[String], ChatContext, Unit]): Unit = {
    if (!commandsSubscribed) {
      onChat(commandsSubscription);
      commandsSubscribed = true;
    }
    if (commands.contains(command)) {
      warn(s"Commands already contains a registration for $command! Overriding...");
    }
    commands += (command -> callback);
  }

  def apiCommands: Seq[APICommand[_]] = Seq.empty;
}
