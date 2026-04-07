package me.ezra_home.retail_software_solution.locations.business.purchase.api

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class PurchaseCreateDto(
  val supplierId: UUID,
  val notes: String? = null,
  val dateOrdered: OffsetDateTime? = null,
  val orderedById: UUID? = null,
  val lines: List<PurchaseLineCreateDto>,
  val stateToken: String? = null
) : Serializable
