package com.example

import cats.effect.IO
import cats.effect.kernel.Ref
import com.example.domain.item.{Item, Title, Price}
import com.example.domain.cart.{Cart, CartTotal, Quantity, SubTotal, Tax, Total}
import com.example.service.{LiveCart, LiveStore}
import munit.CatsEffectSuite
import com.example.domain.store.Store

class ShoppingCartSpec extends CatsEffectSuite {

  lazy val inventory = Store(
    Set(
      Item(Title("Frosties"), Price(4.99)),
      Item(Title("Cheerios"), Price(8.43)),
      Item(Title("Shreddies"), Price(4.68)),
      Item(Title("Weetabix"), Price(9.98)),
      Item(Title("Corn Flakes"), Price(2.52))
    )
  )

  def testCart = for {
    cartStorage <- Ref.of[IO, Map[Item, Quantity]](Map.empty)
    store = LiveStore.make[IO](inventory)
    cart = LiveCart.make[IO](cartStorage, store)
  } yield cart

  test("adding item with no name should leave chart unchanged") {
    for {
      cart <- testCart
      _ <- cart.add(Quantity(1), Title(""))
      c <- cart.view
    } yield {
      assertEquals(c, Cart(Map.empty))
    }
  }

  test("adding item not in inventory to empty cart produces an empty cart") {
    for {
      cart <- testCart
      _ <- cart.add(Quantity(1), Title("toothpaste"))
      c <- cart.view
    } yield {
      assertEquals(c, Cart(Map.empty))
    }
  }

  test("add valid item to empty cart produces cart with valid item") {
    for {
      cart <- testCart
      _ <- cart.add(Quantity(1), Title("Cheerios"))
      c <- cart.view
    } yield {
      assertEquals(
        c,
        Cart(
          Map[Item, Quantity](
            Item(Title("Cheerios"), Price(8.43)) -> Quantity(1)
          )
        )
      )
    }
  }

  test("add multiple items of the same title returns correct cart") {
    for {
      cart <- testCart
      _ <- cart.add(Quantity(1), Title("Cheerios"))
      _ <- cart.add(Quantity(1), Title("Cheerios"))
      c <- cart.view
    } yield {
      assertEquals(
        c,
        Cart(
          Map[Item, Quantity](
            Item(Title("Cheerios"), Price(8.43)) -> Quantity(2)
          )
        )
      )
    }
  }

  test(
    "add multiple valid items to empty cart produces cart with multiple valid items"
  ) {
    for {
      cart <- testCart
      _ <- cart.add(Quantity(1), Title("Cheerios"))
      _ <- cart.add(Quantity(3), Title("Frosties"))
      _ <- cart.add(Quantity(2), Title("Shreddies"))
      _ <- cart.add(Quantity(5), Title("Corn Flakes"))
      c <- cart.view
    } yield {
      assertEquals(
        c,
        Cart(
          Map[Item, Quantity](
            Item(Title("Frosties"), Price(4.99)) -> Quantity(3),
            Item(Title("Cheerios"), Price(8.43)) -> Quantity(1),
            Item(Title("Shreddies"), Price(4.68)) -> Quantity(2),
            Item(Title("Corn Flakes"), Price(2.52)) -> Quantity(5)
          )
        )
      )
    }
  }

  test("correct total with two idential items in cart") {
    for {
      cart <- testCart
      _ <- cart.add(Quantity(1), Title("Cheerios"))
      _ <- cart.add(Quantity(1), Title("Cheerios"))
      c <- cart.total
    } yield {
      assertEquals(
        c,
        CartTotal(
          Cart(Map(Item(Title("Cheerios"), Price(8.43)) -> Quantity(2))),
          SubTotal(16.86),
          Tax(2.11),
          Total(18.97)
        )
      )
    }
  }

  test("correct total with just one item in cart") {
    for {
      cart <- testCart
      _ <- cart.add(Quantity(1), Title("Cheerios"))
      c <- cart.total
    } yield {
      assertEquals(
        c,
        CartTotal(
          Cart(Map(Item(Title("Cheerios"), Price(8.43)) -> Quantity(1))),
          SubTotal(8.43),
          Tax(1.05),
          Total(9.48)
        )
      )
    }
  }

  test("correct total with two items in cart") {
    for {
      cart <- testCart
      _ <- cart.add(Quantity(1), Title("Cheerios"))
      _ <- cart.add(Quantity(1), Title("Frosties"))
      c <- cart.total
    } yield {
      assertEquals(
        c,
        CartTotal(
          Cart(
            Map(
              Item(Title("Cheerios"), Price(8.43)) -> Quantity(1),
              Item(Title("Frosties"), Price(4.99)) -> Quantity(1)
            )
          ),
          SubTotal(13.42),
          Tax(1.68),
          Total(15.10)
        )
      )
    }
  }

  test("double check cart from readme") {
    for {
      cart <- testCart
      _ <- cart.add(Quantity(2), Title("Corn Flakes"))
      _ <- cart.add(Quantity(1), Title("Weetabix"))
      c <- cart.total
    } yield {
      assertEquals(
        c,
        CartTotal(
          Cart(
            Map(
              Item(Title("Corn Flakes"), Price(2.52)) -> Quantity(2),
              Item(Title("Weetabix"), Price(9.98)) -> Quantity(1)
            )
          ),
          SubTotal(15.02),
          Tax(1.88),
          Total(16.90)
        )
      )
    }
  }
}
