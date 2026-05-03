package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBalanceFetcher
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class SaleValidator(
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val salePaymentFetcher: SalePaymentFetcher,
    private val saleStockReserver: SaleStockReserver
) {

    companion object {
        fun guardHasLines(lines: List<*>) {
            if (lines.isEmpty()) throw RtsGenericException("Sale must have at least one line")
        }

        fun guardNoDuplicateProducts(productIds: List<UUID>) {
            if (productIds.size != productIds.toSet().size) {
                throw RtsGenericException("Duplicate products are not allowed in a sale")
            }
        }

        fun guardIsDraft(sale: SaleEntity) {
            if (sale.status != SaleStatus.DRAFT) {
                throw RtsGenericException("Sale ${sale.referenceNumber} is not in DRAFT status")
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

    fun ensureSufficientStockForLines(
        resolvedQuantities: Map<UUID, BigDecimal>,
        productSummaries: Map<UUID, LocationProductSummaryDto>
    ) {
        val locationProductIds = resolvedQuantities.keys.toList()
        val balances = stockBalanceFetcher.getLatestBalances(locationProductIds)
        val reserved = saleStockReserver.getOrLoadFromDbBulk(locationProductIds)
        resolvedQuantities.forEach { (locationProductId, quantity) ->
            val available = (balances[locationProductId] ?: BigDecimal.ZERO).subtract(reserved[locationProductId] ?: BigDecimal.ZERO)
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

    fun guardStockForDraftUpdates(
        requested: Map<UUID, BigDecimal>,
        alreadyReserved: Map<UUID, BigDecimal>,
        summaries: Map<UUID, LocationProductSummaryDto>
    ) {
        if (requested.isEmpty()) return
        val balances = stockBalanceFetcher.getLatestBalances(requested.keys.toList())
        val reserved = saleStockReserver.getOrLoadFromDbBulk(requested.keys)
        requested.forEach { (locationProductId, quantity) ->
            val available = (balances[locationProductId] ?: BigDecimal.ZERO)
                .subtract(reserved[locationProductId] ?: BigDecimal.ZERO)
                .add(alreadyReserved[locationProductId] ?: BigDecimal.ZERO)
            throwIfOverSelling(available, quantity, locationProductId, summaries)
        }
    }

    fun guardWalkInPaymentCoverage(saleId: UUID, saleTotal: BigDecimal) {
        val paid = salePaymentFetcher.calculatePaidAmount(saleId)
        if (paid < saleTotal) throw RtsGenericException("Walk-in sales require full payment coverage")
    }
}
