package com.example.domain

object item {
  case class Price(value: BigDecimal)
  case class Title(value: String)
  case class Item(title: Title, price: Price)
}
