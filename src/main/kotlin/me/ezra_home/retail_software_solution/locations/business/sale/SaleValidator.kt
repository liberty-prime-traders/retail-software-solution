package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.lock.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleUpdateDto
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBalanceFetcher
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
    private val saleStockReserver: SaleStockReserver,
    private val entityAdvisoryLock: EntityAdvisoryLock,
) {

    companion object {
        fun guardHasLines(lines: Collection<*>) {
            if (lines.isEmpty()) throw RtsGenericException("Sale must have at least one line")
        }

        fun guardDateSoldIsNotFuture(dateSold: OffsetDateTime) {
            val orgToday = DateTimes.Local.Now.organization()
            val saleLocalDate = DateTimes.Local.atOrganizationZone(dateSold)
            if (saleLocalDate > orgToday) {
                throw RtsGenericException("Sale date must be today or in the past. Provided date: $saleLocalDate")
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

        fun guardLineIdsBelongToSale(dto: SaleUpdateDto, existingLineIds: Set<UUID>) {
            val invalidRemoves = dto.linesToRemove.filterNot { it in existingLineIds }
            if (invalidRemoves.isNotEmpty()) {
                throw RtsGenericException("Sale line ids to remove do not belong to this sale: $invalidRemoves")
            }
            val updateIds = dto.linesToUpdate.map { it.id }
            val duplicateUpdates = updateIds.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
            if (duplicateUpdates.isNotEmpty()) {
                throw RtsGenericException("Duplicate sale line ids in linesToUpdate: $duplicateUpdates")
            }
            val invalidUpdates = updateIds.filterNot { it in existingLineIds }
            if (invalidUpdates.isNotEmpty()) {
                throw RtsGenericException("Sale line ids to update do not belong to this sale: $invalidUpdates")
            }
            val removeSet = dto.linesToRemove.toHashSet()
            val conflicting = updateIds.filter { it in removeSet }
            if (conflicting.isNotEmpty()) {
                throw RtsGenericException("Sale line ids cannot be both updated and removed: $conflicting")
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
        resolvedBaseQuantitiesPerProduct: Map<UUID, BigDecimal>,
        productSummaries: Map<UUID, LocationProductSummaryDto>
    ) {
        val locationProductIds = resolvedBaseQuantitiesPerProduct.keys.toList()
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, locationProductIds)
        val balances = stockBalanceFetcher.getLatestBalances(locationProductIds)
        val reserved = saleStockReserver.loadReservedTotals(locationProductIds)
        resolvedBaseQuantitiesPerProduct.forEach { (locationProductId, quantity) ->
            val available = balances.getValue(locationProductId).subtract(reserved[locationProductId] ?: BigDecimal.ZERO)
            throwIfOverSelling(available, quantity, locationProductId, productSummaries)
        }
    }

    fun guardStockForDraftUpdates(
        saleId: UUID,
        requested: Map<UUID, BigDecimal>,
        productSummaries: Map<UUID, LocationProductSummaryDto>
    ) {
        if (requested.isEmpty()) return
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, requested.keys)
        val balances = stockBalanceFetcher.getLatestBalances(requested.keys.toList())
        val reservations = saleStockReserver.loadReservationBreakdown(requested.keys)
        requested.forEach { (locationProductId, quantity) ->
            val reservedByOthers = reservations.getValue(locationProductId).excludingSale(saleId)
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


    fun guardWalkInPaymentCoverage(saleId: UUID, saleTotal: BigDecimal) {
        val paid = salePaymentFetcher.calculatePaidAmount(saleId)
        if (paid < saleTotal) throw RtsGenericException("Walk-in sales require full payment coverage")
    }
}
