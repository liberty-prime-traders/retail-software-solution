package me.ezra_home.retail_software_solution.locations.business.purchase.search

import me.ezra_home.retail_software_solution.locations.business.purchase.dto.PurchaseSortField
import me.ezra_home.retail_software_solution.util.enums.PurchaseStatus
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class PurchaseSearchRequest(
    val supplierIds: List<UUID>? = null,
    val orderedByIds: List<UUID>? = null,
    val createdByIds: List<UUID>? = null,
    val statuses: List<PurchaseStatus>? = null,
    val dateOrderedFrom: OffsetDateTime? = null,
    val dateOrderedTo: OffsetDateTime? = null,
    val createdOnFrom: OffsetDateTime? = null,
    val createdOnTo: OffsetDateTime? = null,
    val limit: Int = 50,
    val sortBy: PurchaseSortField = PurchaseSortField.CREATED_ON
) : Serializable
