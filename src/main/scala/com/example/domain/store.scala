package com.example.domain

import com.example.domain.item.{Item, Title}
import com.example.domain.cart.{Cart, CartTotal}

object store {
  case class Store(inventory: Set[Item])

  trait ShoppingStore[F[_]] {
    def lookup(title: Title): F[Option[Item]]
    def checkout(cart: Cart): F[CartTotal]
    def inventory: F[Store]
  }
}
