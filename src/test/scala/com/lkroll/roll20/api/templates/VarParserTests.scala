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
package com.lkroll.roll20.api.templates

import org.scalatest._
import org.scalatest.funsuite._
import org.scalatest.matchers.should.Matchers
import com.lkroll.roll20.core._

class VarParserTests extends AnyFunSuite with Matchers {
  import TemplateVal._
  import CoreImplicits._

  test("A single var should match") {
    val testString = "{{key=value}}";
    val vars = TemplateVars.parse(List(("", List(testString))));
    vars should be (Right(Some(TemplateVars(List(TemplateVar("key", Raw("value")))))));
  }

  test("Empty values should match") {
    val testString = "{{key=}}";
    val vars = TemplateVars.parse(List(("", List(testString))));
    vars should be (Right(Some(TemplateVars(List(TemplateVar("key", Empty))))));
  }

  test("A single var with space should match") {
    val testString = "{{key=value 1}}";
    val vars = TemplateVars.parse(List(("", List(testString))));
    vars should be (Right(Some(TemplateVars(List(TemplateVar("key", Raw("value 1")))))));
  }

  test("A single var with roll-wrapped values should match") {
    val testString = "{{key=[[10]]}}";
    val vars = TemplateVars.parse(List(("", List(testString))));
    vars should be (Right(Some(TemplateVars(List(TemplateVar("key", InlineRoll(Rolls.InlineRoll(10))))))));
  }

  test("A single var with roll-wrapped spaced values should match") {
    val testString = "{{key=[[ 10 ]]}}";
    val vars = TemplateVars.parse(List(("", List(testString))));
    vars should be (Right(Some(TemplateVars(List(TemplateVar("key", InlineRoll(Rolls.InlineRoll(10))))))));
  }

  test("A single var with rollref values should match") {
    val testString = "{{key=$[[10]]}}";
    val vars = TemplateVars.parse(List(("", List(testString))));
    vars should be (Right(Some(TemplateVars(List(TemplateVar("key", InlineRollRef(10)))))));
  }

  test("A single var with translation label should match") {
    val testString = "{{key=^{label}}}";
    val vars = TemplateVars.parse(List(("", List(testString))));
    vars should be (Right(Some(TemplateVars(List(TemplateVar("key", TranslationKey("label")))))));
  }

  test("Multiple vars should match") {
    val testString = "{{key1=value1}} {{key2=value2}} {{key3=value3}}";
    val vars = TemplateVars.parse(List(("", List(testString))));
    val expected = TemplateVars(List(
      TemplateVar("key1", Raw("value1")),
      TemplateVar("key2", Raw("value2")),
      TemplateVar("key3", Raw("value3"))))
    vars should be (Right(Some(expected)));
  }

  test("Multiple vars with extra stuff should match") {
    val testString = "/w gm {{key1=value1}} {{key2=value2}} {{key3=value3}} useless stuff";
    val vars = TemplateVars.parse(List(("", List(testString))));
    val expected = TemplateVars(List(
      TemplateVar("key1", Raw("value1")),
      TemplateVar("key2", Raw("value2")),
      TemplateVar("key3", Raw("value3"))))
    vars should be (Right(Some(expected)));
  }
}
