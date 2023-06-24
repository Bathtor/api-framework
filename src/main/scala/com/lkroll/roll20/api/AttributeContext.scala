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

import com.lkroll.roll20.core.FieldLike

trait AttributeContext {

  /** Fetches or creates an attribute for the given field.
    */
  def attribute[T](field: FieldLike[T]): FieldAttribute[T];

  /** Fetches the attribute for the given field, if it exists.
    *
    * Note that Roll20 does not create attribute objects for fields at their default value!
    */
  def getAttribute[T](field: FieldLike[T]): Option[FieldAttribute[T]];
  // def attributes[T](field: FieldLike[T]): List[FieldAttribute[T]]; // not sure this actually makes any sense
  /** Fetch all instances of this repeating section attribute.
    *
    * Note: As far as I can tell this also works for fields at their default value.
    */
  def repeating[T](field: FieldLike[T]): List[FieldAttribute[T]];

  /** Fetch the instance at `rowId` of this repeating section attribute, if it exists.
    */
  def repeatingAt[T](rowId: String)(field: FieldLike[T]): Option[FieldAttribute[T]];

  /** Fetch all items in all rows of the given repeating section.
    */
  def repeatingSection[T](sectionName: String): List[Attribute];

  def createAttribute[T](field: FieldLike[T]): FieldAttribute[T];
  def createRepeating[T](
      field: FieldLike[T],
      providedRowId: Option[String] = None): FieldAttribute[T];
}

object AttributeMatchers {
  def repeatingSection(sectionName: String): Attribute => Boolean = {
    val pattern = s"""repeating_${sectionName}_[-a-zA-Z0-9]+_.*""".r;
    val nameMatcher: String => Boolean = (s: String) =>
      s match {
        case pattern() => true
        case _         => false
      }
    (a: Attribute) => nameMatcher(a.name)
  }
}

class AttributeCache(private var attributes: List[Attribute], val character: Character)
  extends AttributeContext {
  // TODO build a nice prefix tree from attributes to search this more efficiently

  override def attribute[T](field: FieldLike[T]): FieldAttribute[T] =
    getAttribute(field).orElse {
      val attr = Attribute.create(character.id, field.qualifiedAttr);
      attr.current = field.initialValue;
      attributes ::= attr;
      Some(attr.typed(field))
    }.get;

  override def getAttribute[T](field: FieldLike[T]): Option[FieldAttribute[T]] = {
    val res = attributes.find(a => a.name.equals(field.qualifiedAttr));
    res.map(a => a.typed(field))
  }
  //  override def attributes[T](field: FieldLike[T]): List[FieldAttribute[T]] = {
  //    val res = attributes.filter(a => a.name.equals(field.qualifiedAttr));
  //    res.map(a => a.typed(field))
  //  }
  override def repeating[T](field: FieldLike[T]): List[FieldAttribute[T]] = {
    val nameMatcher = field.nameMatcher;
    val matcher: Attribute => Boolean = (a: Attribute) => nameMatcher(a.name);
    val res = attributes.filter(matcher);
    res.map(a => a.typed(field))
  }
  override def repeatingAt[T](rowId: String)(field: FieldLike[T]): Option[FieldAttribute[T]] = {
    val nameMatcher = field.nameMatcherRow(rowId);
    val matcher: Attribute => Boolean = (a: Attribute) => nameMatcher(a.name);
    val res = attributes.filter(matcher);
    if (res.size > 1) {
      APILogger.warn(
        s"Found more than one instance of ${field.qualifiedAttr} at row=${rowId}. Only returning first.");
    }
    res match {
      case head :: _ => Some(head.typed(field))
      case Nil       => None
    }
  }
  override def repeatingSection[T](sectionName: String): List[Attribute] = {
    val matcher = AttributeMatchers.repeatingSection(sectionName);
    attributes.filter(matcher)
  }
  // TODO Must deal with cache invalidation before implementing these
  override def createAttribute[T](field: FieldLike[T]): FieldAttribute[T] = ???;
  override def createRepeating[T](
      field: FieldLike[T],
      providedRowId: Option[String] = None): FieldAttribute[T] = ???;
}
