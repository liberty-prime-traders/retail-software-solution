package me.ezra_home.retail_software_solution.locations.business.sale.search

import me.ezra_home.retail_software_solution.locations.business.sale.SaleArrears
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleSummary
import java.math.BigDecimal
import java.util.UUID

object SaleSearchMapper {

  fun toRowDto(
    row: SaleSearchRawRow,
    contactNamesById: Map<UUID, String>,
    userNamesById: Map<UUID, String>,
    totalPaid: BigDecimal
  ): SaleSummary = SaleSummary(
    id = row.id,
    referenceNumber = row.referenceNumber,
    contactName = contactNamesById.getValue(row.contactId),
    soldBy = row.soldByUserId?.let { userNamesById.getValue(it) },
    dateSold = row.dateSold,
    status = row.status,
    paymentStatus = row.paymentStatus,
    arrearsTotal = SaleArrears.arrearsTotal(row.status, row.receivableTotal, totalPaid),
    totalPaid = totalPaid
  )
}
