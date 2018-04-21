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

import org.scalatest._
import com.lkroll.roll20.api.conf._

class CommandInvocation extends FunSuite with Matchers {
  import APIImplicits._;

  test("Should invoke simple commands") {
    val button = TestCommand.invoke("testl");
    button.render shouldBe ("[testl](!test)");
  }

  test("Should invoke commands with string args") {
    val button = TestCommand.invoke(
      "testl",
      List("arg1" <<= "testvalue", "arg2" <<= "5"));
    button.render shouldBe ("[testl](!test --arg1 testvalue --arg2 5)");
  }

  test("Should invoke commands with scallop args") {
    val conf = new TestConf(List.empty);
    val button = TestCommand.invoke(
      "testl",
      List(conf.arg1 <<= "testvalue", conf.arg2 <<= false, conf.arg3 <<= 5));
    button.render shouldBe ("[testl](!test --arg1 testvalue  --arg3 5)");

    val button2 = TestCommand.invoke(
      "testl",
      List(conf.arg1 <<= "testvalue", conf.arg2 <<= true, conf.arg3 <<= 5));
    button2.render shouldBe ("[testl](!test --arg1 testvalue --arg2 --arg3 5)");
  }

  test("Should invoke commands with scallop list args") {
    val conf = new TestConf(List.empty);
    val button = TestCommand.invoke(
      "testl",
      List(conf.arg1 <<= "testvalue", conf.arg4 <<= "t1", conf.arg4 <<= "t2", conf.arg5 <<= 5, conf.arg5 <<= 6));
    button.render shouldBe ("[testl](!test --arg1 testvalue --arg4 t1 --arg4 t2 --arg5 5 --arg5 6)");
  }
}

object TestCommand extends APICommand[TestConf] {
  override def command: String = "test";
  override def options: Function[Array[String], TestConf] = (args) => new TestConf(args);
  override def apply(config: TestConf, ctx: ChatContext): Unit = {
    ???
  }
}

class TestConf(_args: Seq[String]) extends ScallopAPIConf(_args) {
  val arg1 = opt[String]("arg1");
  val arg2 = opt[Boolean]("arg2");
  val arg3 = opt[Int]("arg3");
  val arg4 = opt[List[String]]("arg4");
  val arg5 = opt[List[Int]]("arg5");
  verify();
}
