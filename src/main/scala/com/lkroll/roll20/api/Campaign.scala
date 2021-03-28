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
import scalajs.js.JSON
import com.lkroll.roll20.api.facade.Roll20API
import collection.mutable
import util.{Failure, Success, Try}

object Campaign {
  def apply(): Campaign = new Campaign(Roll20API.Campaign());

  /** The default colour token markers.
    *
    * These aren't returned by `Campaign.tokenMarkers`.
    *
    * The values are: "red", "blue", "green", "brown", "purple", "pink", "yellow".
    */
  val colourMarkers: List[TokenMarker] = List(
    TokenMarker(id = -1, name = "Red", tag = "red", url = ""),
    TokenMarker(id = -1, name = "Blue", tag = "blue", url = ""),
    TokenMarker(id = -1, name = "Green", tag = "green", url = ""),
    TokenMarker(id = -1, name = "Brown", tag = "brown", url = ""),
    TokenMarker(id = -1, name = "Purple", tag = "purple", url = ""),
    TokenMarker(id = -1, name = "Pink", tag = "pink", url = ""),
    TokenMarker(id = -1, name = "Yellow", tag = "yellow", url = "")
  );

  object Properties {
    val turnorder = "turnorder";
    val initiativepage = "initiativepage";
    val playerpageid = "playerpageid";
    val playerspecificpages = "playerspecificpages";
    val journalfolder = "_journalfolder";
    val jukeboxfolder = "_jukeboxfolder";
    val tokenmarkers = "token_markers";
  }
}

class Campaign(val raw: Roll20API.Roll20Object) extends Roll20Managed {
  import Campaign._;

  lazy val turnOrder: TurnOrder = new TurnOrder(this);

  /** ID of the page used for the tracker when the turn order window is open.
    *
    * When set to false, the turn order window closes.
    */
  def initiativePage: Option[String] = {
    raw.get(Properties.initiativepage).asInstanceOf[String] match {
      case "false" => None
      case s       => Some(s)
    }
  }
  def initiativePage_=(v: Option[String]): Unit = {
    val s = v.getOrElse("false");
    raw.set(Properties.initiativepage, s);
  }

  /** ID of the page the player bookmark is set to.
    *
    * Players see this page by default, unless overridden by `playerSpecificPages`.
    */
  def playerPageId: Option[String] = {
    raw.get(Properties.playerpageid).asInstanceOf[String] match {
      case "false" => None
      case s       => Some(s)
    }
  }
  def playerPageId_=(v: Option[String]): Unit = {
    val s = v.getOrElse("false");
    raw.set(Properties.playerpageid, s);
  }
  // TODO

  /** A map from player id to page id.
    *
    * Any player set to a page in this object will override the `playerPageId`.
    */
  def playerSpecificPages: Map[String, String] = {
    try {
      val jsonMap = raw.get(Properties.playerspecificpages).asInstanceOf[js.Dictionary[String]];
      jsonMap.toMap
    } catch {
      case e: Throwable => Map.empty // could be false instead of object (stupid JS untyped APIs -.-)
    }
  }
  // TODO
  // _journalfolder	A JSON string which contains data about the folder structure of the game. Read-only.
  // _jukeboxfolder		A JSON string which contains data about the jukebox playlist structure of the game. Read-only.

  /** List of all token markers in the campaign.
    *
    * Read-only.
    *
    * @return A list of all token markers in the campaign.
    */
  def tokenMarkers: List[TokenMarker] = {
    try {
      val stringifiedArray = raw.get(Properties.tokenmarkers).asInstanceOf[String];
      val parsed = js.JSON.parse(stringifiedArray).asInstanceOf[js.Array[js.Dynamic]];
      parsed.toList.map(entry =>
        TokenMarker(entry.id.asInstanceOf[Int],
                    entry.name.asInstanceOf[String],
                    entry.tag.asInstanceOf[String],
                    entry.url.asInstanceOf[String]
        )
      )
    } catch {
      case e: Throwable => Nil
    }
  }
}

case class TokenMarker(id: Int, name: String, tag: String, url: String)

object TurnOrder {
  sealed trait Entry {
    def id: String;
    def pr: Either[Int, String];
    def prString: String =
      pr match {
        case Left(i)  => i.toString
        case Right(s) => s
      };
    def custom: String;
    def write(): String = s"""{"id" : "$id", "pr" : "$prString", "custom" : "$custom"}""";
  }
  case class CustomEntry(custom: String, pr: Either[Int, String]) extends Entry {
    override def id: String = "-1";
  }
  case class TokenEntry(id: String, pr: Either[Int, String]) extends Entry {
    override def custom: String = "";
  }

  def readEntry(raw: js.Dynamic): Try[Entry] = {
    Try {
      if (raw.id.asInstanceOf[String] == "-1") {
        CustomEntry(raw.custom.asInstanceOf[String], readPr(raw.pr))
      } else {
        TokenEntry(raw.id.asInstanceOf[String], readPr(raw.pr))
      }
    }
  }

  def readPr(raw: js.Dynamic): Either[Int, String] = {
    val s = raw.toString; // could be either a number or a string
    Try(s.trim.toInt) match {
      case Success(i) => Left(i)
      case Failure(_) => Right(s)
    }
  }
}

class TurnOrder(private val campaign: Campaign) {
  import TurnOrder._;
  import Campaign._;

  def get(): List[Entry] = {
    val jsonString = campaign.raw.get(Properties.turnorder).asInstanceOf[String];
    APILogger.debug(s"Read turnoder to '$jsonString'");
    val jsonArray = JSON.parse(jsonString).asInstanceOf[js.Array[js.Dynamic]];
    val tried = jsonArray.toList.map(o => TurnOrder.readEntry(o));
    tried.flatMap {
      case Success(e) => Some(e)
      case Failure(e) => APILogger.error(e); None
    }
  }

  def set(l: List[Entry]): Unit = {
    val jsonString = l.map(_.write()).mkString("[", ",", "]");
    APILogger.debug(s"About to set turnoder to '$jsonString'");
    campaign.raw.set(Properties.turnorder, jsonString);
  }

  def modify(f: List[Entry] => List[Entry]): Unit = set(f(get()));

  def clear(): Unit = {
    modify(_ => Nil)
  }

  def next(): Unit = {
    modify {
      case head :: rest => rest ++ List(head)
      case Nil          => Nil
    }
  }

  def peek(): Option[Entry] = {
    get() match {
      case head :: _ => Some(head)
      case Nil       => None
    }
  }

  def ::=(e: Entry): Unit = {
    modify(l => e :: l)
  }

  def :::=(el: List[Entry]): Unit = {
    modify(l => el ::: l)
  }

  def +=(e: Entry): Unit = {
    modify(l => l :+ e)
  }

  def ++=(el: List[Entry]): Unit = {
    modify(l => l ++ el)
  }

  def sortAsc(): Unit = {
    modify(_.sortWith({
      case (left, right) => {
        (left.pr, right.pr) match {
          case (Left(li), Left(ri))   => li < ri
          case (Left(li), Right(rs))  => li.toString < rs
          case (Right(ls), Left(ri))  => ls < ri.toString
          case (Right(ls), Right(rs)) => ls < rs
        }
      }
    }))
  }

  def sortDesc(): Unit = {
    modify(_.sortWith({
      case (left, right) => {
        (left.pr, right.pr) match {
          case (Left(li), Left(ri))   => li > ri
          case (Left(li), Right(rs))  => li.toString > rs
          case (Right(ls), Left(ri))  => ls > ri.toString
          case (Right(ls), Right(rs)) => ls > rs
        }
      }
    }))
  }

  def dedup(keepFirst: Boolean = false): Unit = {
    modify { l =>
      val tokens = mutable.Map.empty[String, TokenEntry];
      val customs = mutable.Map.empty[String, CustomEntry];
      l.foreach {
        case te: TokenEntry =>
          if (keepFirst) {
            if (!tokens.contains(te.id)) {
              tokens += (te.id -> te)
            }
          } else {
            tokens += (te.id -> te)
          }
        case ce: CustomEntry =>
          if (keepFirst) {
            if (!customs.contains(ce.id)) {
              customs += (ce.id -> ce)
            }
          } else {
            customs += (ce.id -> ce)
          }
      }
      (tokens.values ++ customs.values).toList
    }
  }
}
