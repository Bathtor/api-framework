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

import com.lkroll.roll20.api._
import fastparse.all._

case class TemplateVar(key: String, value: Option[String]) {
  def render: String = s"""{{$key=${renderValue}}}""";
  def renderValue: String = value.getOrElse("");
  def stripValue: Option[String] = value.map { v =>
    if (v.startsWith("[[")) {
      v.substring(2, v.length() - 2)
    } else {
      v
    }
  }
}

object TemplateVar {
  val parser = P("{{" ~ CharsWhile(c => c != '=' && c != '}').! ~ "=" ~/ CharsWhile(_ != '}').!.? ~ "}}")
    .map(t => TemplateVar(t._1, t._2));
  def fromString(s: String): Option[TemplateVar] = {
    parser.parse(s) match {
      case Parsed.Success(r, _) => Some(r)
      case _: Parsed.Failure    => None
    }
  }
}

case class TemplateVars(vars: List[TemplateVar]) {
  def render: String = vars.map(_.render).mkString(" ");
  def replaceInlineRolls(rolls: List[InlineRoll]): TemplateVars = {
    val res = vars.map { tvar =>
      val newval = tvar.value.map { v =>
        TemplateVars.inlineRollRefParser.parse(v) match {
          case Parsed.Success(r, _) => {
            val ir = rolls(r);
            val total = ir.results.total;
            s"[[${total.toString()}]]"
          }
          case _ => v
        }
      };
      TemplateVar(tvar.key, newval)
    };
    TemplateVars(res)
  }
  def lookup(key: String): Option[TemplateVar] = {
    vars.foreach { tvar =>
      if (tvar.key == key) {
        return Some(tvar);
      }
    }
    return None;
  }
  def ::(tvar: TemplateVar): TemplateVars = TemplateVars(tvar :: vars);
}

object TemplateVars extends org.rogach.scallop.ValueConverter[TemplateVars] {
  import org.rogach.scallop._;

  val parser = P(CharsWhile(_ != '{').rep ~ (TemplateVar.parser ~ CharsWhileIn(" ").rep).rep ~/ AnyChar.rep)
    .map(s => TemplateVars(s.toList));

  val inlineRollRefParser = P("$[[" ~ CharIn('0' to '9').rep(1).! ~ "]]").map(_.toInt);

  def parse(s: List[(String, List[String])]): Either[String, Option[TemplateVars]] =
    s match {
      case (_, l) :: Nil => {
        val s = l.mkString(" "); // reassemble the arg list the way it was before splitting
        parser.parse(s) match {
          case Parsed.Success(r, _) => Right(Some(r))
          case f: Parsed.Failure    => Left(f.toString())
        }
      }
      case Nil => Right(None) // no vars found
      case _   => Left("provide template vars") // error when parsing
    }

  override val argType = ArgType.LIST;
}
