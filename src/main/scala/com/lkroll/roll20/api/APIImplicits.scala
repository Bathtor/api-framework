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

import com.lkroll.roll20.core.{ CoreImplicits, PrimitiveStringSerialisers, Renderable }
import org.rogach.scallop._
import com.lkroll.roll20.api.conf._

trait APIImplicits extends CoreImplicits with PrimitiveStringSerialisers {
  implicit def token2Entry(l: List[(Token, Int)]): List[TurnOrder.Entry] = l.map(t => TurnOrder.TokenEntry(t._1.id, Left(t._2)));

  implicit class ApplicableOption[T](opt: ScallopOption[T]) {
    def <<=(v: T): OptionApplication = {
      v match {
        case s: String  => AppliedOption(opt.asInstanceOf[ScallopOption[String]], s, DefaultOptionRenderers.str)
        case b: Boolean => BooleanOption(opt.asInstanceOf[ScallopOption[Boolean]], b)
        case _          => AppliedOption(opt, v, DefaultOptionRenderers.trivial[T])
      }
    }
    def <<=[S](v: S)(implicit ev: T =:= List[S]): OptionApplication = {
      v match {
        case s: String => ListOption(opt.asInstanceOf[ScallopOption[List[String]]], s, DefaultOptionRenderers.str)
        case _         => ListOption(opt.asInstanceOf[ScallopOption[List[S]]], v, DefaultOptionRenderers.trivial[S])
      }
    }
  }

  implicit class StringApplicableOption(opt: String) {
    def <<=(v: String): OptionApplication = StringAppliedOption(opt, v);
  }
}

object APIImplicits extends APIImplicits;