package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.SaleAdjustmentFetcher
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentFetcher
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionHeader
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SessionIdentity
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SaleSessionLoader(
    private val saleDataFetcher: SaleDataFetcher,
    private val saleAdjustmentFetcher: SaleAdjustmentFetcher,
    private val salePaymentFetcher: SalePaymentFetcher,
    private val domainToSessionMapper: DomainToSessionMapper,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val saleSessionStockOverlay: SaleSessionStockOverlay,
    private val saleSessionValidator: SaleSessionValidator,
) {

    fun newSession(sessionId: UUID, contactId: UUID): SaleSession {
        val emptyHeader = SaleSessionHeader(
            contactId = contactId,
            soldById = null,
            dateSold = null,
            notes = null,
            referenceNumber = null
        )
        return finish(SaleSessionBuilder(sessionId).withHeader(emptyHeader))
    }

    fun copyFromSale(sessionId: UUID, sourceSaleId: UUID): SaleSession {
        val sourceSaleHeader = saleDataFetcher.getSaleHeader(sourceSaleId)
        saleSessionValidator.guardSourceSaleCopyable(sourceSaleHeader.status)
        val sourceSaleLines = saleDataFetcher.getSaleLines(sourceSaleId)
        val sourceSaleAdjustments = saleAdjustmentFetcher.getAdjustments(sourceSaleId)

        val baseUnitIdsByLocationProductId = locationProductDataFetcher.getBaseUnitIds(
            sourceSaleLines.map { it.locationProductId }
        )
        val copiedSaleLines = sourceSaleLines.map { sourceSaleLine ->
            domainToSessionMapper.toSaleSessionLine(sourceSaleLine, baseUnitIdsByLocationProductId)
                .copy(identity = SessionIdentity.mintFreshIdentity())
        }
        val copiedLineIdentityBySourceLineId = sourceSaleLines.zip(copiedSaleLines)
            .associate { (sourceSaleLine, copiedSaleLine) -> sourceSaleLine.id to copiedSaleLine.identity }
        val copiedSaleAdjustments = sourceSaleAdjustments.map { sourceSaleAdjustment ->
            domainToSessionMapper.toSaleSessionAdjustment(sourceSaleAdjustment, copiedLineIdentityBySourceLineId)
                .copy(identity = SessionIdentity.mintFreshIdentity())
        }
        val copiedHeader = SaleSessionHeader(
            contactId = sourceSaleHeader.contactId,
            soldById = null,
            dateSold = null,
            notes = sourceSaleHeader.notes,
            referenceNumber = null,
        )

        return finish(
            SaleSessionBuilder(sessionId)
                .withHeader(copiedHeader)
                .withSaleLines(copiedSaleLines)
                .withSaleAdjustments(copiedSaleAdjustments)
                .withLinesAwaitingReservation()
        )
    }

    fun loadFromSale(sessionId: UUID, saleId: UUID): SaleSession {
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

        return finish(
            SaleSessionBuilder(sessionId)
                .linkedToPersistedSale(saleHeader.id, saleHeader.version, saleHeader.status)
                .withHeader(domainToSessionMapper.toSaleSessionHeader(saleHeader))
                .withSaleLines(saleSessionLines)
                .withSaleAdjustments(saleSessionAdjustments)
                .withSalePayments(saleSessionPayments)
        )
    }

    private fun finish(saleSessionBuilder: SaleSessionBuilder): SaleSession =
        SaleSessionTotalsCalculator.recompute(saleSessionStockOverlay.populate(saleSessionBuilder.build()))
}
