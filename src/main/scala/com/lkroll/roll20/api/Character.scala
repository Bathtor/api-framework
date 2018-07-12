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
import java.net.URL

object Character {
  def create(): Character = {
    val c = js.Dynamic.literal().asInstanceOf[CharacterCreate];
    val cObj = createObj(Roll20ObjectTypes.character, c);
    assert(cObj.get(Roll20Managed.Properties.`type`).asInstanceOf[String] == Roll20ObjectTypes.character);
    new Character(cObj)
  }

  def create(name: String): Character = {
    val c = js.Dynamic.literal(name = name).asInstanceOf[CharacterCreate];
    val cObj = createObj(Roll20ObjectTypes.character, c);
    assert(cObj.get(Roll20Managed.Properties.`type`).asInstanceOf[String] == Roll20ObjectTypes.character);
    new Character(cObj)
  }

  def get(id: String): Option[Character] = {
    val cObj = getObj(Roll20ObjectTypes.character, id).toOption;
    cObj.map(o => new Character(o))
  }

  object Properties {
    val avatar = "avatar";
    val name = "name";
    val bio = "bio";
    val gmnotes = "gmnotes";
    val archived = "archived";
    val inplayerjournals = "inplayerjournals";
    val controlledby = "controlledby";
    val defaulttoken = "_defaulttoken";
  }
}

class Character private (val raw: Roll20Object) extends Roll20Managed with AttributeContext {
  import Character._;

  /**
   * URL to an image used for the character.
   *
   * See the note about avatar and imgsrc restrictions
   * at [[https://wiki.roll20.net/API:Objects#imgsrc_and_avatar_property_restrictions Roll20 Docs]].
   */
  def avatar: URL = {
    val urlS = raw.get(Properties.avatar).asInstanceOf[String];
    new URL(urlS)
  }
  def avatar_=(url: URL): Unit = {
    val urlS = url.toString;
    raw.set(Properties.avatar, urlS)
  }
  def name: String = raw.get(Properties.name).asInstanceOf[String];
  def name_=(s: String): Unit = raw.set(Properties.name, s);
  // TODO bio	""	The character's biography. See the note below about accessing the Notes, GMNotes, and bio fields.

  // TODO gmnotes	""	Notes on the character only viewable by the GM. See the note below about accessing the Notes, GMNotes, and bio fields.
  def archived: Boolean = raw.get(Properties.archived).asInstanceOf[Boolean];
  def archived_=(b: Boolean): Unit = raw.set(Properties.archived, b);
  /**
   * Comma-delimited list of player ID who can view this character.
   *
   * Use "all" to give all players the ability to view.
   */
  def inPlayerJournals: List[String] = {
    val s = raw.get(Properties.inplayerjournals).asInstanceOf[String];
    s.split(",").toList
  }
  def inPlayerJournals_=(l: List[String]): Unit = {
    val s = l.mkString(",");
    raw.set(Properties.inplayerjournals, s);
  }

  /**
   * Comma-delimited list of player IDs who can control and edit this character.
   *
   * Use "all" to give all players the ability to edit.
   */
  def controlledBy: List[String] = {
    val s = raw.get(Properties.controlledby).asInstanceOf[String];
    s.split(",").toList
  }
  def controlledBy_=(l: List[String]): Unit = {
    val s = l.mkString(",");
    raw.set(Properties.controlledby, s);
  }

  // TODO _defaulttoken	""	A JSON string that contains the data for the Character's default token if one is set. Note that this is a "blob" similar to "bio" and "notes", so you must pass a callback function to get(). Read-only.

  def attributesForName(name: String): List[Attribute] = Attribute.find(name, this.id);
  /**
   * Gets the current value of the field indicated by `name`.
   *
   * Works for fields at default value.
   */
  def attributeValue[T](name: String): Option[Any] = {
    getAttrByName(this.id, name).toOption
  }
  /**
   * Gets the current value of the `field`, converted to `T`.
   *
   * Works for fields at default value.
   *
   * @return `None` if either the field does not exist, or the conversion failed, and `Some(T)` otherwise.
   */
  def attributeValue[T](field: FieldLike[T]): Option[T] = {
    getAttrByName(this.id, field.qualifiedAttr).toOption.flatMap(s => field.read(s.toString))
  }

  override def attribute[T](field: FieldLike[T]): FieldAttribute[T] = getAttribute(field).orElse {
    val attr = Attribute.create(this.id, field.qualifiedAttr);
    attr.current = field.initialValue;
    Some(attr.typed(field))
  } get;
  override def getAttribute[T](field: FieldLike[T]): Option[FieldAttribute[T]] = Attribute.findSingle(field, this.id);
  //override def attributes[T](field: FieldLike[T]): List[FieldAttribute[T]] = Attribute.findAll(field, this.id);
  override def repeating[T](field: FieldLike[T]): List[FieldAttribute[T]] = Attribute.findRepeating(field, this.id);
  override def repeatingAt[T](rowId: String)(field: FieldLike[T]): Option[FieldAttribute[T]] = Attribute.findRepeating(field, this.id, rowId);
  override def repeatingSection[T](sectionName: String): List[Attribute] = Attribute.findRepeating(sectionName, this.id);
  override def createAttribute[T](field: FieldLike[T]): FieldAttribute[T] = Attribute.create(this.id, field);
  override def createRepeating[T](field: FieldLike[T], providedRowId: Option[String] = None): FieldAttribute[T] = Attribute.createRepeating(this.id, field, providedRowId);

  def cached(): AttributeCache = {
    val query = js.Dynamic.literal(
      "type" -> Roll20ObjectTypes.attribute,
      Attribute.Properties.characterid -> this.id);
    val res = findObjs(query).toList.map(o => new Attribute(o));
    new AttributeCache(res, this)
  }
  def withCache(f: AttributeCache => Unit): Unit = {
    f(this.cached());
  }
  def abilitiesForName(name: String): List[Ability] = Ability.find(name, this.id);
  def abilities: List[Ability] = Ability.findAll(this.id);

  override def toString(): String = s"Character(${js.JSON.stringify(raw)})";
}

