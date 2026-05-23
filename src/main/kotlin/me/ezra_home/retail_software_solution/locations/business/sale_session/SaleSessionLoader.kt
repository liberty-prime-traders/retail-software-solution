package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.SaleAdjustmentFetcher
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentFetcher
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionHeader
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionTotals
import me.ezra_home.retail_software_solution.util.business.DateTimes
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SaleSessionLoader(
    private val saleDataFetcher: SaleDataFetcher,
    private val saleAdjustmentFetcher: SaleAdjustmentFetcher,
    private val salePaymentFetcher: SalePaymentFetcher,
    private val saleSessionTotalsCalculator: SaleSessionTotalsCalculator,
    private val domainToSessionMapper: DomainToSessionMapper,
    private val locationProductDataFetcher: LocationProductDataFetcher,
) {

    fun newSession(
        sessionId: UUID,
        locationId: UUID,
        contactId: UUID,
        userId: UUID,
    ): SaleSession {
        val now = DateTimes.Offset.Now.organization()
        val saleSession = SaleSession(
            sessionId = sessionId,
            locationId = locationId,
            saleId = null,
            saleVersion = null,
            originalStatus = SaleStatus.DRAFT,
            createdById = userId,
            createdAt = now,
            lastUpdatedAt = now,
            lastAccessedById = userId,
            lastAccessedAt = now,
            header = SaleSessionHeader(
                contactId = contactId,
                soldById = null,
                dateSold = null,
                notes = null,
                referenceNumber = null
            ),
            saleLines = emptyList(),
            saleAdjustments = emptyList(),
            salePayments = emptyList(),
            totals = SaleSessionTotals.ZERO,
        )
        return saleSessionTotalsCalculator.recompute(saleSession)
    }

    fun loadFromSale(sessionId: UUID, saleId: UUID): SaleSession {
        val now = DateTimes.Offset.Now.organization()
        val userId = SessionContextProvider.getUserId()
        val locationId = SessionContextProvider.getLocationId()
        val saleHeader = saleDataFetcher.getSaleHeader(saleId)
        val saleLineSnapshots = saleDataFetcher.getSaleLines(saleId)
        val saleAdjustmentSnapshots = saleAdjustmentFetcher.getAdjustments(saleId)
        val salePaymentSnapshots = salePaymentFetcher.getPaymentSnapshots(saleId)

        val baseUnitIdsByLocationProductId = locationProductDataFetcher.getBaseUnitIds(
            saleLineSnapshots.map { it.locationProductId }
        )
        val saleSessionLines = saleLineSnapshots.map { saleLineDto ->
            domainToSessionMapper.toSaleSessionLine(saleLineDto, baseUnitIdsByLocationProductId)
        }
        val relatedSaleLineIdentityBySaleLineId = saleSessionLines.associate { it.identity.id!! to it.identity }
        val saleSessionAdjustments = saleAdjustmentSnapshots.map { adjustmentSnapshot ->
            domainToSessionMapper.toSaleSessionAdjustment(adjustmentSnapshot, relatedSaleLineIdentityBySaleLineId)
        }
        val saleSessionPayments = salePaymentSnapshots.map(domainToSessionMapper::toSaleSessionPayment)

        val saleSession = SaleSession(
            sessionId = sessionId,
            locationId = locationId,
            saleId = saleHeader.id,
            saleVersion = saleHeader.version,
            originalStatus = saleHeader.status,
            createdById = userId,
            createdAt = now,
            lastUpdatedAt = now,
            lastAccessedById = userId,
            lastAccessedAt = now,
            header = domainToSessionMapper.toSaleSessionHeader(saleHeader),
            saleLines = saleSessionLines,
            saleAdjustments = saleSessionAdjustments,
            salePayments = saleSessionPayments,
            totals = SaleSessionTotals.ZERO,
        )
        return saleSessionTotalsCalculator.recompute(saleSession)
    }
}
