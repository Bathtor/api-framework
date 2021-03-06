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
import java.net.URL

object Graphic {

  //  def fromRaw(raw: Roll20Object): Graphic = {
  //    assert(raw.get(Properties.`type`).asInstanceOf[String] == Roll20ObjectTypes.graphic);
  //    raw.get(Properties.subtype).asInstanceOf[String] match {
  //      case Subtypes.token => new Token(raw)
  //      case Subtypes.card  => new Card(raw);
  //    }
  //  }

  def get(id: String): Option[Graphic] = {
    val rawO = getObj(Roll20ObjectTypes.graphic, id).toOption;
    rawO.map { raw =>
      raw.get(Properties.subtype).asInstanceOf[String] match {
        case Subtypes.token => new Token(raw)
        case Subtypes.card  => new Card(raw)
        case x              => throw new RuntimeException(s"Illegal Graphic type: $x")
      }
    }
  }

  object Subtypes {
    val token = "token";
    val card = "card";
  }

  object Properties {
    val subtype = "_subtype";
    val cardid = "_cardid";
    val pageid = "_pageid";
    val imgsrc = "imgsrc";
    val bar1Link = "bar1_link";
    val bar2Link = "bar2_link";
    val bar3Link = "bar3_link";
    val represents = "represents";
    val left = "left";
    val top = "top";
    val width = "width";
    val height = "height";
    val rotation = "rotation";
    val layer = "layer";
    val isdrawing = "isdrawing";
    val flipv = "flipv";
    val fliph = "fliph";
    val name = "name";
    val gmnotes = "gmnotes";
    val controlledby = "controlledby";
    val bar1Value = "bar1_value";
    val bar2Value = "bar2_value";
    val bar3Value = "bar3_value";
    val bar1Max = "bar1_max";
    val bar2Max = "bar2_max";
    val bar3Max = "bar3_max";
    val aura1Radius = "aura1_radius";
    val aura2Radius = "aura2_radius";
    val aura1Color = "aura1_color";
    val aura2Color = "aura2_color";
    val aura1Square = "aura1_square";
    val aura2Square = "aura2_square";
    val tintColor = "tint_color";
    val statusmarkers = "statusmarkers";
    val showname = "showname"
    val playerseditName = "playersedit_name";
    val playerseditBar1 = "playersedit_bar1";
    val playerseditBar2 = "playersedit_bar2";
    val playerseditBar3 = "playersedit_bar3";
    val playerseditAura1 = "playersedit_aura1";
    val playerseditAura2 = "playersedit_aura2";
    val showplayersName = "showplayers_name";
    val showplayersBar1 = "showplayers_bar1";
    val showplayersBar2 = "showplayers_bar2";
    val showplayersBar3 = "showplayers_bar3";
    val showplayersAura1 = "showplayers_aura1";
    val showplayersAura2 = "showplayers_aura2";
    val lightRadius = "light_radius";
    val LightDimRadius = "light_dimradius";
    val lightOtherPlayers = "light_otherplayers";
    val lightHasSight = "light_hassight";
    val lightAngle = "light_angle";
    val lightLoSAngle = "light_losangle";
    val lastMove = "lastmove";
    val lightMultiplier = "light_multiplier";
  }
}

sealed abstract class Graphic protected (val raw: Roll20Object) extends Roll20Managed {
  import Graphic._;

  /**
    * May be "token" (for tokens and maps) or "card" (for cards). Read-only.
    */
  def subtype: String = raw.get(Properties.subtype).asInstanceOf[String];

  /**
    * ID of the page the object is in. Read-only.
    */
  def pageId: String = raw.get(Properties.pageid).asInstanceOf[String];

  /**
    * The URL of the graphic's image.
    * See the note about avatar and imgsrc restrictions
    * at [[https://wiki.roll20.net/API:Objects#imgsrc_and_avatar_property_restrictions Roll20 Docs]].
    */
  def imgSrc: URL = {
    val urlS = raw.get(Properties.imgsrc).asInstanceOf[String];
    new URL(urlS)
  }
  def imgSrc_=(url: URL): Unit = {
    val urlS = url.toString;
    raw.set(Properties.imgsrc, urlS)
  }

  def name: String = raw.get(Properties.name).asInstanceOf[String];
  def name_=(s: String): Unit = raw.set(Properties.name, s);

  /**
    * List currently active status markers.
    *
    * @return A list of currently active status markers.
    */
  def statusMarkers: List[String] = {
    val stringified = raw.get(Properties.statusmarkers).asInstanceOf[String];
    val parsed = stringified.split(",");
    if (parsed.head.isEmpty()) {
      Nil
    } else {
      parsed.toList
    }
  };
  def statusMarkers_=(l: List[String]): Unit = {
    val stringified = l.mkString(",");
    raw.set(Properties.statusmarkers, stringified);
  }

  /**
    * Same as `statusMarkers` but without the counts.
    *
    * @return A list of currently active status markers with counts removed.
    */
  def statusMarkersStripped: List[String] = {
    statusMarkers.map(s => s.split("@").head)
  }

  // TODO the rest oO
}

class Card(_raw: Roll20Object) extends Graphic(_raw) {
  import Graphic._;

  /**
    * Set to an ID if the graphic is a card. Read-only.
    */
  def cardId: String = raw.get(Properties.cardid).asInstanceOf[String];

  // TODO the rest oO

  override def toString(): String = s"Card(${js.JSON.stringify(raw)})";
}

class Token(_raw: Roll20Object) extends Graphic(_raw) {
  import Graphic._;

  /**
    * ID of the character this token represents.
    */
  def representsId: String = raw.get(Properties.represents).asInstanceOf[String];
  def representsId_=(s: String): Unit = raw.set(Properties.represents, s);

  /**
    * The character this token represents.
    */
  def represents: Option[Character] = {
    val cid = representsId;
    if (cid.isEmpty()) {
      None
    } else {
      Character.get(representsId)
    }
  }
  def represents_=(c: Character) = {
    representsId = c.id;
  }

  // TODO the rest oO

  override def toString(): String = s"Token(${js.JSON.stringify(raw)})";
}
