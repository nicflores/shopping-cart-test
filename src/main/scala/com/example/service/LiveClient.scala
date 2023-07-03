package com.example.service

import cats.Parallel
import cats.effect._
import cats.implicits._
import com.example.domain.store.Store
import com.example.domain.item.{Title, Item, Price}
import com.example.domain.client.{
  InventoryClient,
  InventoryItem,
  ItemRequestError
}
import io.circe.Decoder
import io.circe.generic.semiauto._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.Uri
import org.http4s.Method
import org.http4s.Request
import org.http4s.Status

object LiveClient {

  def make[F[_]: Concurrent: Parallel](client: Client[F]): InventoryClient[F] =
    new InventoryClient[F] with Http4sClientDsl[F] {

      implicit val iiDecoder: Decoder[InventoryItem] = deriveDecoder
      implicit val iiEntityDecoder: EntityDecoder[F, InventoryItem] =
        jsonOf[F, InventoryItem]
      implicit val itemRequestErrorDecoder: Decoder[ItemRequestError] =
        deriveDecoder
      implicit def itemRequestErrorEntityDecoder[F[_]: Concurrent]
          : EntityDecoder[F, ItemRequestError] = jsonOf[F, ItemRequestError]

      val supplies: List[Title] = List(
        Title("cheerios"),
        Title("cornflakes"),
        Title("frosties"),
        Title("shreddies"),
        Title("weetabix")
      )
      def getInventoryItem(
          title: Title
      ): F[Either[ItemRequestError, InventoryItem]] = {
        val target =
          s"https://raw.githubusercontent.com/mattjanks16/shopping-cart-test-data/main/${title.value}.json"
        Uri
          .fromString(target)
          .liftTo[F]
          .flatMap { uri =>
            client
              .run(Request(Method.GET, uri))
              .use { resp =>
                resp.status match {
                  case Status.Ok =>
                    resp.as[InventoryItem].map(_.asRight[ItemRequestError])
                  case s: Status =>
                    ItemRequestError(s.reason).asLeft[InventoryItem].pure[F]
                  // resp.as[ItemRequestError].map(_.asLeft[InventoryItem])
                }
              }
              .recoverWith { case t: Throwable =>
                ItemRequestError(t.getMessage).asLeft[InventoryItem].pure[F]
              }
          }
      }
      def inventory: F[Store] = {
        supplies
          .parTraverse(getInventoryItem)
          .flatMap(x =>
            x.sequence.map(l =>
              Store(l.map(ii => Item(Title(ii.titl), Price(ii.price))).toSet)
            ) match {
              case Left(_)      => Store(Set.empty).pure[F]
              case Right(store) => store.pure[F]
            }
          )
      }
    }
}
