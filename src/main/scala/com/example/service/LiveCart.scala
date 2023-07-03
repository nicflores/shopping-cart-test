package com.example.service

import cats.Monad
import cats.implicits._
import cats.effect.kernel.Ref
import com.example.domain.item.{Item, Title}
import com.example.domain.cart.{Cart, CartTotal, Quantity, ShoppingCart}
import com.example.domain.store.ShoppingStore

object LiveCart {
  def make[F[_]: Monad](
      ref: Ref[F, Map[Item, Quantity]],
      store: ShoppingStore[F]
  ): ShoppingCart[F] = new ShoppingCart[F] {

    def removeItem(title: Title): F[Unit] = for {
      item <- store.lookup(title)
      _ <- item match {
        case None => ().pure[F]
        case Some(i) =>
          ref.update(m => {
            if (m.contains(i))
              m.updatedWith(i) {
                case None                    => None
                case Some(q) if q.value == 1 => (m - i); None
                case Some(q)                 => Some(Quantity(q.value - 1))
              }
            else m
          })
      }
    } yield ()

    def add(quantity: Quantity, title: Title): F[Unit] = for {
      item <- store.lookup(title)
      _ <- item match {
        case None => ().pure[F]
        case Some(i) =>
          ref.update(m => {
            if (m.contains(i))
              m.updatedWith(i) {
                case Some(q) => Some(Quantity(quantity.value + q.value))
                case None    => None
              }
            else m + (i -> quantity)
          })
      }
    } yield ()
    def total: F[CartTotal] = ref.get.flatMap(m => store.checkout(Cart(m)))
    def view: F[Cart] = ref.get.flatMap(m => Cart(m).pure[F])
  }
}
