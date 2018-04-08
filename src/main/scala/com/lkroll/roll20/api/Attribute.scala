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
import com.lkroll.roll20.api.facade.Roll20API._
import com.lkroll.roll20.api.facade.Roll20Objects._
import com.lkroll.roll20.core.{ FieldLike, StringSerialiser }

object Attribute {
  def create(characterId: String, name: String): Attribute = {
    val attr = js.Dynamic.literal(_characterid = characterId, name = name).asInstanceOf[AttributeCreate];
    val attrObj = createObj(Roll20ObjectTypes.attribute, attr);
    assert(attrObj.get(Roll20Managed.Properties.`type`).asInstanceOf[String] == Roll20ObjectTypes.attribute);
    new Attribute(attrObj)
  }

  def create[T](characterId: String, field: FieldLike[T]): FieldAttribute[T] = {
    val attr = create(characterId, field.qualifiedAttr);
    attr.typed(field)
  }

  def createRepeating[T](characterId: String, field: FieldLike[T], providedRowId: Option[String] = None): FieldAttribute[T] = {
    val rowId = providedRowId match {
      case Some(s) => s
      case None    => APIUtils.generateRowId()
    };
    val name = field.accessor(rowId);
    val attr = create(characterId, name);
    attr.typed(field)
  }

  def get(id: String): Option[Attribute] = {
    val attrObj = getObj(Roll20ObjectTypes.attribute, id).toOption;
    attrObj.map(o => new Attribute(o))
  }

  private def findRaw(name: String, characterId: String): List[Roll20Object] = {
    val query = js.Dynamic.literal(
      "type" -> Roll20ObjectTypes.attribute,
      Properties.name -> name,
      Properties.characterid -> characterId);
    val res = findObjs(query);
    res.toList
  }

  def find(name: String, characterId: String): List[Attribute] = {
    val res = findRaw(name, characterId);
    res.map(o => new Attribute(o))
  }

  def findSingle[T](field: FieldLike[T], characterId: String): Option[FieldAttribute[T]] = {
    val res = findRaw(field.qualifiedAttr, characterId);
    if (res.size > 1) {
      APILogger.warn(s"Skipping ${res.size - 1} attribute results and returning head.");
    }
    res match {
      case head :: _ => Some(new FieldAttribute(field, head))
      case Nil       => None
    }
  }

  def findAll[T](field: FieldLike[T], characterId: String): List[FieldAttribute[T]] = {
    val res = findRaw(field.qualifiedAttr, characterId);
    res.map(o => new FieldAttribute(field, o))
  }

  def findMatching(fieldMatcher: Attribute => Boolean, characterId: String): List[Attribute] = {
    val query = js.Dynamic.literal(
      "type" -> Roll20ObjectTypes.attribute,
      Properties.characterid -> characterId);
    val res = findObjs(query).toList;
    res.map(o => new Attribute(o)).filter(fieldMatcher)
  }

  def findRepeating[T](field: FieldLike[T], characterId: String): List[FieldAttribute[T]] = {
    val nameMatcher = field.nameMatcher;
    val matcher: Attribute => Boolean = (a: Attribute) => nameMatcher(a.name);
    val res = findMatching(matcher, characterId);
    res.map(a => a.typed(field))
  }

  def findRepeating[T](field: FieldLike[T], characterId: String, rowId: String): Option[FieldAttribute[T]] = {
    val nameMatcher = field.nameMatcherRow(rowId);
    val matcher: Attribute => Boolean = (a: Attribute) => nameMatcher(a.name);
    val res = findMatching(matcher, characterId);
    if (res.size > 1) {
      APILogger.warn(s"Found more than one instance of ${field.qualifiedAttr} at row=${rowId}. Only returning first.");
    }
    res match {
      case head :: _ => Some(head.typed(field))
      case Nil       => None
    }
  }

  object Properties {
    val characterid = "_characterid";
    val name = "name";
    val current = "current";
    val max = "max";
  }

  val rowIdPattern = raw"repeating_[a-zA-Z]+_([-a-zA-Z0-9]+)_.*".r;
}

class Attribute private[api] (val raw: Roll20Object) extends Roll20Managed {
  import Attribute._;

  /**
   * ID of the character this attribute belongs to. Read-only.
   */
  def characterId: String = raw.get(Properties.characterid).asInstanceOf[String];
  def character: Character = Character.get(characterId).get;

  def name: String = raw.get(Properties.name).asInstanceOf[String];
  def name_=(s: String): Unit = raw.set(Properties.name, s);

  /**
   * Try to extract the rowId from the name, if this is an attribute in a repeating section.
   */
  def getRowId: Option[String] = name match {
    case rowIdPattern(rowId) => Some(rowId)
    case _                   => None
  }

  /**
   * The current value of the attribute can be accessed in chat and macros with the syntax
   * @{Character Name|Attribute Name} or in abilities with the syntax @{Attribute Name}.
   */
  def current: String = raw.get(Properties.current).asInstanceOf[String];
  def current_=(s: String): Unit = raw.set(Properties.current, s);
  /**
   * The max value of the attribute can be accessed in chat and macros with the syntax
   * @{Character Name|Attribute Name|max} or in abilities with the syntax @{Attribute Name|max}.
   */
  def max: String = raw.get(Properties.max).asInstanceOf[String];
  def max_=(s: String): Unit = raw.set(Properties.max, s);

  override def toString(): String = s"Attribute(${js.JSON.stringify(raw)})";

  /**
   * Returns this attribute as typed by the given field.
   */
  def typed[T](f: FieldLike[T]): FieldAttribute[T] = {
    //assert(this.name == f.name); // doesn't hold for repeating sections
    new FieldAttribute(f, this.raw);
  }
}

/**
 * Typed version of an attribute, based on SheetModel fields.
 *
 * @tparam T type of the attribute
 */
class FieldAttribute[T] private[api] (val field: FieldLike[T], _raw: Roll20Object) extends Attribute(_raw) {

  /**
   * Get the current value of the attributed converted to `T` via the field's readable instance.
   */
  def apply(): T = {
    val rawV = if (field.isMax) { super.max } else { super.current };
    val vO = field.read(rawV);
    vO.get
  }

  /**
   * Get the current value of the attributed converted to `T` via the field's readable instance, as on `Option`
   * in case the conversion doesn't work (or the field is actually empty).
   */
  def get: Option[T] = {
    val rawV = if (field.isMax) { super.max } else { super.current };
    field.read(rawV)
  }

  /**
   * Update the current value, converting `T` to string via the provided
   * [[com.lkroll.roll20.core.StringSerialiser]].
   */
  def <<=(t: T)(implicit ser: StringSerialiser[T]): Unit = {
    val stringV = ser.serialise(t);
    if (field.isMax) {
      super.max = stringV;
    } else {
      super.current = stringV;
    };

  }
}
