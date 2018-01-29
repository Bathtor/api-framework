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

class Character private (val raw: Roll20Object) extends Roll20Managed {
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
  def abilitiesForName(name: String): List[Ability] = Ability.find(name, this.id);

  override def toString(): String = s"Character(${js.JSON.stringify(raw)})";
}

object Attribute {
  def create(characterId: String, name: String): Attribute = {
    val attr = js.Dynamic.literal(_characterid = characterId, name = name).asInstanceOf[AttributeCreate];
    val attrObj = createObj(Roll20ObjectTypes.attribute, attr);
    assert(attrObj.get(Roll20Managed.Properties.`type`).asInstanceOf[String] == Roll20ObjectTypes.attribute);
    new Attribute(attrObj)
  }

  def get(id: String): Option[Attribute] = {
    val attrObj = getObj(Roll20ObjectTypes.attribute, id).toOption;
    attrObj.map(o => new Attribute(o))
  }

  def find(name: String, characterId: String): List[Attribute] = {
    val query = js.Dynamic.literal(
      "type" -> Roll20ObjectTypes.attribute,
      Properties.name -> name,
      Properties.characterid -> characterId);
    val res = findObjs(query);
    res.map(o => new Attribute(o)).toList
  }

  object Properties {
    val characterid = "_characterid";
    val name = "name";
    val current = "current";
    val max = "max";
  }
}

class Attribute private (val raw: Roll20Object) extends Roll20Managed {
  import Attribute._;

  /**
   * ID of the character this attribute belongs to. Read-only.
   */
  def characterId: String = raw.get(Properties.characterid).asInstanceOf[String];
  def character: Character = Character.get(characterId).get;

  def name: String = raw.get(Properties.name).asInstanceOf[String];
  def name_=(s: String): Unit = raw.set(Properties.name, s);
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
}

object Ability {
  def create(characterId: String, name: String): Ability = {
    val ab = js.Dynamic.literal(_characterid = characterId, name = name).asInstanceOf[AbilityCreate];
    val abObj = createObj(Roll20ObjectTypes.ability, ab);
    assert(abObj.get(Roll20Managed.Properties.`type`).asInstanceOf[String] == Roll20ObjectTypes.ability);
    new Ability(abObj)
  }

  def get(id: String): Option[Ability] = {
    val abObj = getObj(Roll20ObjectTypes.ability, id).toOption;
    abObj.map(o => new Ability(o))
  }

  def find(name: String, characterId: String): List[Ability] = {
    val query = js.Dynamic.literal(
      "type" -> Roll20ObjectTypes.ability,
      Properties.name -> name,
      Properties.characterid -> characterId);
    val res = findObjs(query);
    res.map(o => new Ability(o)).toList
  }

  object Properties {
    val characterid = "_characterid";
    val name = "name";
    val description = "description";
    val action = "action";
    val istokenaction = "istokenaction";
  }
}

class Ability private (val raw: Roll20Object) extends Roll20Managed {
  import Ability._;

  /**
   * ID of the character this ability belongs to. Read-only.
   */
  def characterId: String = raw.get(Properties.characterid).asInstanceOf[String];
  def character: Character = Character.get(characterId).get;
  def name: String = raw.get(Properties.name).asInstanceOf[String];
  def name_=(s: String): Unit = raw.set(Properties.name, s);
  /**
   * The description does not appear in the character sheet interface.
   */
  def description: String = raw.get(Properties.description).asInstanceOf[String];
  def description_=(s: String): Unit = raw.set(Properties.description, s);
  /**
   * The text of the ability.
   */
  def action: String = raw.get(Properties.action).asInstanceOf[String];
  def action_=(s: String): Unit = raw.set(Properties.action, s);
  /**
   * Is this ability a token action that should show up when tokens linked to its parent Character are selected?
   */
  def isTokenAction: Boolean = raw.get(Properties.istokenaction).asInstanceOf[Boolean];
  def isTokenAction_=(b: Boolean): Unit = raw.set(Properties.istokenaction, b);

  override def toString(): String = s"Ability(${js.JSON.stringify(raw)})";
}
