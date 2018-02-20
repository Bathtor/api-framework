package com.lkroll.roll20.api

import org.scalatest._

class AttributeTests extends FunSuite with Matchers {
  val attrs = List(
    "repeating_testsec_-KkWmmPeaGP87vaZLpkt_testsec_testf",
    "repeating_testsec_-uKudbakP63yteZLerta_testsec_testf");

  test("Should extract rowId") {
    val res = attrs.head match {
      case Attribute.rowIdPattern(rowId) => Some(rowId)
      case _                             => None
    };
    res should be (Some("-KkWmmPeaGP87vaZLpkt"));
  }
}
