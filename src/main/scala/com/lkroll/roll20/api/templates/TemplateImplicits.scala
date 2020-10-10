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

import com.lkroll.roll20.core._

trait TemplateImplicits extends TemplateCoreImplicits {
  def templateV[T](t: (String, T))(implicit conv: T => TemplateVal): TemplateVar = {
    val v = conv(t._2);
    TemplateVar(t._1, v)
  }
  implicit def ot2tv[T](tO: Option[T])(implicit conv: T => TemplateVal): TemplateVal = tO match {
    case Some(t) => conv(t)
    case None    => TemplateVal.Empty
  }
  implicit def unit2tv(u: Unit): TemplateVal = TemplateVal.Empty;
  implicit def str2tv(s: String): TemplateVal = TemplateVal.Raw(s);
  implicit def num2tv[N: Numeric](n: N): TemplateVal = TemplateVal.Number(n);
  implicit def bool2tv(b: Boolean): TemplateVal = TemplateVal.BooleanVal(b);
  implicit def roll2tv(r: Rolls.InlineRoll[Int]): TemplateVal = TemplateVal.InlineRoll(r);
  implicit def rollx2tv(r: RollExpression[Int]): TemplateVal = TemplateVal.InlineRoll(r);
  implicit def btn2tv(b: APIButton): TemplateVal = TemplateVal.APIButton(b);

  implicit class TemplateRefExt(t: TemplateRef) {
    def fillWith(vars: TemplateVar*): TemplateApplication = {
      TemplateApplication(t, vars)
    }
    def fillWith(vars: Iterable[TemplateVar]): TemplateApplication = {
      TemplateApplication(t, vars)
    }
    def fillWith(vars: TemplateVars): TemplateApplication = {
      TemplateApplication(t, vars)
    }
  }
}

object TemplateImplicits extends TemplateImplicits;
