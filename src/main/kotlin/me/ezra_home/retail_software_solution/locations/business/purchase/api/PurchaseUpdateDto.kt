package me.ezra_home.retail_software_solution.locations.business.purchase.api

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
  val linesToAdd: List<PurchaseLineCreateDto> = emptyList(),
  val linesToUpdate: List<PurchaseLineUpdateDto> = emptyList()
) : Serializable
