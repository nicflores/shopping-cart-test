package com.example

import cats.effect.IO
import cats.implicits._
import munit.CatsEffectSuite
import com.example.domain.item.{Item, Price, Title}
import com.example.service.{LiveClient, LiveStore}
import com.example.domain.store
import org.http4s.ember.client.EmberClientBuilder
import com.example.domain.item

class ShoppingStoreSpec extends CatsEffectSuite {

  def testStore = for {
    clientResource <- EmberClientBuilder
      .default[IO]
      .build
      .map(LiveClient.make[IO])
      .pure[IO]
    inv <- clientResource.use(c => c.inventory)
    store = LiveStore.make[IO](inv)
  } yield store

  lazy val testInventory =
    store.Store(
      Set(
        Item(item.Title("Frosties"), Price(4.99)),
        Item(item.Title("Cheerios"), Price(8.43)),
        Item(item.Title("Shreddies"), Price(4.68)),
        Item(item.Title("Weetabix"), Price(9.98)),
        Item(item.Title("Corn Flakes"), Price(2.52))
      )
    )

  test("make sure store inventory is correct") {
    for {
      s <- testStore
      inv <- s.inventory
    } yield assertEquals(inv, testInventory)
  }

  test("correctly lookup store item") {
    for {
      s <- testStore
      res <- s.lookup(Title("Cheerios"))
    } yield assertEquals(res, Some(Item(Title("Cheerios"), Price(8.43))))
  }

  test("failed lookup returns None") {
    for {
      s <- testStore
      res <- s.lookup(Title("Cheetos"))
    } yield assertEquals(res, None)
  }

}
