package com.example.service

import cats.implicits._
import com.example.domain.store.{Store, ShoppingStore}
import com.example.domain.item.{Title, Item}
import com.example.domain.cart.{Cart, CartTotal, SubTotal, Tax, Total}
import java.text.DecimalFormat
import cats.Applicative

object LiveStore {

  def make[F[_]: Applicative](store: Store): ShoppingStore[F] =
    new ShoppingStore[F] {

      def inventory: F[Store] = store.pure[F]

      def lookup(title: Title): F[Option[Item]] = {
        store.inventory.find(i => i.title == title).pure[F]
      }

      def checkout(cart: Cart): F[CartTotal] = {
        val formatter = new DecimalFormat("#.00")
        val c = cart.items
          .foldLeft(CartTotal(cart, SubTotal(0), Tax(0), Total(0))) {
            case (ct, (i, q)) => {
              ct.copy(subtotal =
                SubTotal(ct.subtotal.value + (i.price.value * q.value))
              )
            }
          }
        val sub = BigDecimal(formatter.format(c.subtotal.value))
        val tax = BigDecimal(formatter.format(sub * 0.125))
        val total = BigDecimal(formatter.format(tax + sub))
        CartTotal(c.cart, SubTotal(sub), Tax(tax), Total(total)).pure[F]
      }
    }
}
