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
import fastparse.WhitespaceApi

object ArithmeticParsers {
  val White = WhitespaceApi.Wrapper{
    import fastparse.all._
    NoTrace(" ".rep)
  }
  import fastparse.noApi._
  import White._

  val intExpression: P[ArithmeticExpression[Int]] = P(intLiteral);
  val floatExpression: P[ArithmeticExpression[Float]] = P(floatLiteral);
  val intLiteral: P[Arith.Literal[Int]] = P(CharIn('0' to '9').rep(1).!).map(s => Arith.Literal(s.toInt));
  val floatLiteral: P[Arith.Literal[Float]] = P((CharIn('0' to '9').rep(1) ~ "." ~/ CharIn('0' to '9').rep(1)).!).map(s => Arith.Literal(s.toFloat));
  // TODO all the rest oO

  def parseInt(s: String): Parsed[ArithmeticExpression[Int]] = intExpression.parse(s);
}