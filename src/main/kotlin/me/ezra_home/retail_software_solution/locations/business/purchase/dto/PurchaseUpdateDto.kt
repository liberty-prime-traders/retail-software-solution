package me.ezra_home.retail_software_solution.locations.business.purchase.dto

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

data class PurchaseUpdateDto(
  val id: UUID,
  val supplierId: UUID? = null,
  val notes: Optional<String>? = null,
  val dateOrdered: Optional<OffsetDateTime>? = null,
  val orderedById: Optional<UUID>? = null,
  val lines: List<PurchaseLineUpdateDto>
) : Serializable
