package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBalanceFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockReserver
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class SaleValidator(
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val salePaymentFetcher: SalePaymentFetcher,
    private val stockReserver: StockReserver,
    private val entityAdvisoryLock: EntityAdvisoryLock,
) {

    companion object {
        fun guardDateSoldIsNotFuture(dateSold: OffsetDateTime) {
            val orgToday = DateTimes.Local.Now.organization()
            val saleLocalDate = DateTimes.Local.atOrganizationZone(dateSold)
            if (saleLocalDate > orgToday) {
                throw RtsGenericException("Sale date must be today or in the past. Provided date: $saleLocalDate")
            }
        }
    }

    fun guardCanVoid(sale: SaleEntity) {
        if (sale.status == SaleStatus.VOIDED) {
            throw RtsGenericException("Sale ${sale.referenceNumber} is already voided")
        }
        if (sale.status == SaleStatus.DISCARDED) {
            throw RtsGenericException("Sale ${sale.referenceNumber} is discarded and cannot be voided")
        }
        if (salePaymentFetcher.hasActivePayments(sale.id!!)) {
            throw RtsGenericException("Cannot void a sale with active payments")
        }
    }

    fun guardSufficientStockForSale(
        saleId: UUID,
        requested: Map<UUID, BigDecimal>,
        productSummaries: Map<UUID, LocationProductSummaryDto>
    ) {
        if (requested.isEmpty()) return
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, requested.keys)
        val balances = stockBalanceFetcher.getLatestBalances(requested.keys.toList())
        val reservations = stockReserver.loadReservationBreakdown(requested.keys)
        requested.forEach { (locationProductId, quantity) ->
            val reservedByOthers = reservations[locationProductId]?.excludingSale(saleId) ?: BigDecimal.ZERO
            val available = balances.getValue(locationProductId).subtract(reservedByOthers)
            throwIfOverSelling(available, quantity, locationProductId, productSummaries)
        }
    }

    fun throwIfOverSelling(
        available: BigDecimal,
        quantity: BigDecimal,
        locationProductId: UUID,
        productSummaries: Map<UUID, LocationProductSummaryDto>
    ) {
        if (available < quantity) {
            val label = productSummaries[locationProductId]?.label ?: locationProductId.toString()
            val formattedAvailable = Decimals.stripZeroesAndRound(available)
            val formattedRequested = Decimals.stripZeroesAndRound(quantity)
            throw RtsGenericException("Insufficient stock for $label. Available: $formattedAvailable, Requested: $formattedRequested")
        }
    }

}
