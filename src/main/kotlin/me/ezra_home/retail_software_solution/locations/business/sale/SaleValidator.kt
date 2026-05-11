package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineCreateDto
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
    private val saleStockReserver: SaleStockReserver,
    private val productReservationLock: ProductReservationLock,
) {

    companion object {
        fun guardHasLines(lines: Collection<*>) {
            if (lines.isEmpty()) throw RtsGenericException("Sale must have at least one line")
        }

        fun guardWalkInPaymentCoverage(
            dto: SaleCreateDto,
            productSummaries: Map<UUID, LocationProductSummaryDto>,
        ) {
            if (!dto.walkInCustomer) return
            val saleTotal = dto.linesToAdd.sumOf { line ->
                val unitPrice = productSummaries.getValue(line.locationProductId).unitPrice
                    ?: throw RtsGenericException("Product ${line.locationProductId} has no unit price")
                Decimals.multiplyScale4(line.quantity, unitPrice)
            }
            if (dto.payments.sumOf { it.amount } < saleTotal) {
                throw RtsGenericException("Walk-in sales require full payment coverage")
            }
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
        productReservationLock.acquire(locationProductIds)
        val balances = stockBalanceFetcher.getLatestBalances(locationProductIds)
        val reserved = saleStockReserver.loadReservedTotals(locationProductIds)
        resolvedQuantities.forEach { (locationProductId, quantity) ->
            val available = balances.getValue(locationProductId).subtract(reserved[locationProductId] ?: BigDecimal.ZERO)
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
        saleId: UUID,
        requested: Map<UUID, BigDecimal>,
        productSummaries: Map<UUID, LocationProductSummaryDto>
    ) {
        if (requested.isEmpty()) return
        productReservationLock.acquire(requested.keys)
        val balances = stockBalanceFetcher.getLatestBalances(requested.keys.toList())
        val reservations = saleStockReserver.loadReservationBreakdown(requested.keys)
        requested.forEach { (locationProductId, quantity) ->
            val reservedByOthers = reservations.getValue(locationProductId).excludingSale(saleId)
            val available = balances.getValue(locationProductId).subtract(reservedByOthers)
            throwIfOverSelling(available, quantity, locationProductId, productSummaries)
        }
    }

    fun guardWalkInPaymentCoverage(saleId: UUID, saleTotal: BigDecimal) {
        val paid = salePaymentFetcher.calculatePaidAmount(saleId)
        if (paid < saleTotal) throw RtsGenericException("Walk-in sales require full payment coverage")
    }
}
