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
import Roll20API.{ Roll20Object, ChatMessage }
import com.lkroll.roll20.core.{ DefaultSerialiser, Serialiser, FieldLike }
import com.lkroll.roll20.util.ListMultiMap
import scala.scalajs.js.Dynamic.{ global => dynGlobal, literal => dynLiteral }
import scala.concurrent.{ Future, Promise, ExecutionContext }
import collection.mutable

@JSExportDescendentObjects
trait APIScriptRoot extends APIScript {
  def children: Seq[APIScript] = Seq.empty;
  def readyChildren: Seq[APIScript] = Seq.empty;

  @JSExport
  def load() {
    val allChildren = (children ++ readyChildren);
    val aggFieldS = allChildren.foldLeft(this.fieldSerialisers)((acc, child) => acc ++ child.fieldSerialisers);
    val aggTypeS = allChildren.foldLeft(this.typeSerialisers)((acc, child) => acc ++ child.typeSerialisers);
    this.fieldSerialisers = aggFieldS;
    this.typeSerialisers = aggTypeS;
    allChildren.foreach { child =>
      child.fieldSerialisers = aggFieldS;
      child.typeSerialisers = aggTypeS;
    }
    subscriptions.foreach { t: (String, Seq[js.Function]) =>
      {
        val (k, callbacks) = t;
        callbacks.foreach { c =>
          debug(s"${this.getClass.getName}: subscribing callback on trigger: ${k}.");
          Roll20API.on(k, c)
        }
      }
    }
    children.foreach(_.internal_load());
    debug("------ Registered Serialisers -------");
    fieldSerialisers.foreach {
      case (f, s) => debug(s"${f} -> ${s.getClass.getName}")
    }
    typeSerialisers.foreach {
      case (t, s) => debug(s"${t} -> ${s.getClass.getName}")
    }
  }

  onReady {
    val allChildren = (children ++ readyChildren);
    allChildren.foreach(_.ready = true);
    readyChildren.foreach(_.internal_load());
  }
}

trait APIScript extends APILogging with APIUtils {
  import js.JSConverters._

  implicit val ec: ExecutionContext = scala.scalajs.concurrent.JSExecutionContext.runNow;

  val subscriptions = new mutable.HashMap[String, mutable.MutableList[js.Function]] with ListMultiMap[String, js.Function];
  val commands = new mutable.HashMap[String, Function2[Array[String], ChatContext, Unit]];
  private var commandsSubscribed: Boolean = false;
  private val commandsSubscription: Function1[ChatMessage, Unit] = (msg) => {
    if (msg.`type` == "api") {
      val chatctx = ChatContext.fromMsg(msg);
      val args = msg.content.split("\\s+");
      assert(args.length >= 1);
      assert(args(0).startsWith("!"));
      val cmd = args(0).substring(1);
      commands.get(cmd) match {
        case Some(handler) => handler(args, chatctx);
        case None          => debug(s"No handler found for command ${cmd} in ${args.mkString(" ")}")
      }
    }
  }

  private[api] var fieldSerialisers = Map.empty[String, Serialiser[Any]];
  private[api] var typeSerialisers = List.empty[(Class[Any], Serialiser[Any])];
  val defaultSerialiser = DefaultSerialiser;

  private[api] var ready: Boolean = false;
  def isReady: Boolean = this.ready;

  private[api] def internal_load() {
    apiCommands.foreach { c =>
      debug(s"${this.getClass.getName}: registering command ${c.command}");
      onCommand(c.command)(c.callback);
    }
    subscriptions.foreach { t: (String, Seq[js.Function]) =>
      {
        val (k, callbacks) = t;
        callbacks.foreach { c =>
          debug(s"${this.getClass.getName}: subscribing callback on trigger: ${k}.");
          Roll20API.on(k, c)
        }
      }
    }
  }

  def register[T](f: FieldLike[T], s: Serialiser[T]) {
    fieldSerialisers += (f.accessor -> s.asInstanceOf[Serialiser[Any]]); // just throw away the type info
  }

  def register[T: reflect.ClassTag](s: Serialiser[T]) {
    val staticClass: Class[Any] = reflect.classTag[T].runtimeClass.asInstanceOf[Class[Any]];
    typeSerialisers ::= (staticClass -> s.asInstanceOf[Serialiser[Any]])
  }

  private def checkTypeHierarchies(cls: Class[_]): Option[Serialiser[Any]] = {
    typeSerialisers.find({
      case (targetCls, ser) => targetCls.isAssignableFrom(cls)
    }).map(_._2)
  }

  def serialise[T](f: FieldLike[T], v: T): js.Any = {
    fieldSerialisers.get(f.accessor) match {
      case Some(s) => s.serialise(v)
      case None => checkTypeHierarchies(v.getClass()) match {
        case Some(s) => s.serialise(v)
        case None    => defaultSerialiser.serialise(v)
      }
    }
  }

  def onReady(callback: => Unit): Unit = {
    subscriptions.addBinding("ready", (callback _));
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
