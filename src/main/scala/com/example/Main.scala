package com.example

import cats.implicits._
import cats.effect.IOApp
import cats.effect.IO
import org.http4s.ember.client.EmberClientBuilder
import com.example.service.LiveClient
import cats.effect.kernel.Ref
import com.example.domain.item.{Item}
import com.example.domain.cart.Quantity
import com.example.service.{LiveCart, LiveStore}
import com.example.domain.item.Title

object Main extends IOApp.Simple {

  def run: IO[Unit] =
    for {
      cartStorage <- Ref.of[IO, Map[Item, Quantity]](Map.empty)
      clientResource <- EmberClientBuilder
        .default[IO]
        .build
        .map(LiveClient.make[IO])
        .pure[IO]
      inv <- clientResource.use(c => c.inventory)
      store = LiveStore.make[IO](inv)
      cart = LiveCart.make[IO](cartStorage, store)
      _ <- cart.add(Quantity(1), Title("Toothpaste"))
      _ <- cart.add(Quantity(1), Title("Cheerios"))
      _ <- cart.add(Quantity(2), Title("Corn Flakes"))
      _ <- cart.add(Quantity(10), Title("Frosties"))
      _ <- cart.add(Quantity(1), Title("Cheerios"))
      viewcart <- cart.view
      total <- cart.total
    } yield {
      println(viewcart)
      println(total)
    }
}
