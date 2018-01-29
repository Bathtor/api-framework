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
import com.lkroll.roll20.core._
import fastparse.all._

sealed trait TemplateVal extends Renderable;
object TemplateVal {
  case object Empty extends TemplateVal {
    override def render: String = "";
  }
  case class Raw(s: String) extends TemplateVal {
    override def render: String = s;
  }
  case class TranslationKey(key: String) extends TemplateVal {
    override def render: String = s"^{$key}";
  }
  case class InlineRoll(r: Rolls.InlineRoll[Int]) extends TemplateVal {
    override def render: String = r.render;
  }
  object InlineRoll {
    def apply(expr: RollExpression[Int]): InlineRoll = InlineRoll(Rolls.InlineRoll(expr));
  }
  case class InlineRollRef(index: Int) extends TemplateVal {
    override def render: String = "$[[" + index + "]]";
  }
  case class Number[N: Numeric](v: N) extends TemplateVal {
    override def render: String = v.toString();
  }
}

case class TemplateVar(key: String, value: TemplateVal) extends Renderable {
  override def render: String = s"""{{$key=${value.render}}}""";
}

object TemplateVar {
  import TemplateVal._;
  import CoreImplicits._;

  val parser = P("{{" ~ keyParser ~ "=" ~/ valueParser ~ "}}").map(t => TemplateVar(t._1, t._2));
  val keyParser: P[String] = P(CharsWhile(c => c != '=' && c != '{' && c != '}').!);
  val valueParser: P[TemplateVal] = P(emptyValueParser | translationLabelParser | inlineRollParser | inlineRollRefParser | rawParser);

  val emptyValueParser: P[TemplateVal] = P(&("}}")).map(_ => Empty);
  val inlineRollRefParser: P[TemplateVal] = P("$[[" ~/ ws ~ CharIn('0' to '9').rep(1).! ~ ws ~ "]]").map(s => InlineRollRef(s.toInt));
  val inlineRollParser: P[TemplateVal] = P("[[" ~/ ws ~ ArithmeticParsers.intExpression ~ ws ~ "]]").map(e => InlineRoll(e));
  val translationLabelParser: P[TemplateVal] = P("^{" ~/ CharsWhile(_ != '}').rep.! ~ "}").map(s => TranslationKey(s));
  val rawParser: P[TemplateVal] = P(CharsWhile(c => c != '=' && c != '{' && c != '}' && c != '^').!).map(s => Raw(s));

  val ws = P(" ".rep);

  def fromString(s: String): Option[TemplateVar] = {
    parser.parse(s) match {
      case Parsed.Success(r, _) => Some(r)
      case _: Parsed.Failure    => None
    }
  }
}

case class TemplateVars(vars: List[TemplateVar]) extends Renderable {
  override def render: String = vars.map(_.render).mkString(" ");
  def replaceInlineRollRefs(rolls: List[InlineRoll], transformer: InlineRoll => TemplateVal): TemplateVars = {
    val trans2: Function2[InlineRoll, TemplateVar, TemplateVal] = (ir, tv) => transformer(ir);
    replaceInlineRollRefs(rolls, trans2)
  }
  def replaceInlineRollRefs(rolls: List[InlineRoll], transformer: (InlineRoll, TemplateVar) => TemplateVal): TemplateVars = {
    val res = vars.map { tvar =>
      val newval = tvar.value match {
        case TemplateVal.InlineRollRef(index) => {
          val ir = rolls(index);
          transformer(ir, tvar)
        }
        case x => x
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

  val parser = P(CharsWhile(_ != '{').rep ~ (TemplateVar.parser ~ CharsWhileIn(" ").?).rep ~/ AnyChar.rep)
    .map(s => TemplateVars(s.toList));

  def parse(s: List[(String, List[String])]): Either[String, Option[TemplateVars]] =
    s match {
      case (_, l) :: Nil => {
        val s = l.mkString(" "); // reassemble the arg list the way it was before splitting
        parser.parse(s) match {
          case Parsed.Success(r, _) => Right(Some(r))
          case f: Parsed.Failure    => System.err.println(f.extra.traced.trace); Left(f.toString())
        }
      }
      case Nil => Right(None) // no vars found
      case _   => Left("provide template vars") // error when parsing
    }

  override val argType = ArgType.LIST;
}
