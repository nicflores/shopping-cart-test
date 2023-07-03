package com.example.domain

import com.example.domain.item.{Item, Title}

object cart {
  case class SubTotal(value: BigDecimal)
  case class Tax(value: BigDecimal)
  case class Total(value: BigDecimal)
  case class Quantity(value: Int)
  case class Cart(items: Map[Item, Quantity])
  case class CartTotal(cart: Cart, subtotal: SubTotal, tax: Tax, total: Total)

  trait ShoppingCart[F[_]] {
    def removeItem(title: Title): F[Unit]
    def add(quantity: Quantity, title: Title): F[Unit]
    def total: F[CartTotal]
    def view: F[Cart]
  }
}
