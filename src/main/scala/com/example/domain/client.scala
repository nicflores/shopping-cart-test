package com.example.domain

import com.example.domain.store.Store
import com.example.domain.item.Title

object client {
  case class ItemRequestError(message: String) extends Exception
  case class InventoryItem(titl: String, price: Double)

  trait InventoryClient[F[_]] {
    val supplies: List[item.Title]
    def getInventoryItem(
        title: Title
    ): F[Either[ItemRequestError, InventoryItem]]
    def inventory: F[Store]
  }
}
