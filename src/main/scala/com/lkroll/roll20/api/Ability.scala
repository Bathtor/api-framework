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

  def findAll(characterId: String): List[Ability] = {
    val query = js.Dynamic.literal(
      "type" -> Roll20ObjectTypes.ability,
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
