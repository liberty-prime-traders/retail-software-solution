package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.SaleAdjustmentFetcher
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentFetcher
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionHeader
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionPayment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionTotals
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SessionIdentity
import me.ezra_home.retail_software_solution.util.business.DateTimes
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SaleSessionLoader(
    private val saleDataFetcher: SaleDataFetcher,
    private val saleAdjustmentFetcher: SaleAdjustmentFetcher,
    private val salePaymentFetcher: SalePaymentFetcher,
    private val saleSessionTotalsCalculator: SaleSessionTotalsCalculator,
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

        val saleSessionLines = saleLineSnapshots.map { lineSnapshot ->
            SaleSessionLine(
                identity = SessionIdentity.persisted(lineSnapshot.id),
                locationProductId = lineSnapshot.locationProductId,
                productLabel = lineSnapshot.productLabel,
                quantity = lineSnapshot.quantity,
                unitId = lineSnapshot.unitId,
                conversionFactor = lineSnapshot.conversionFactor,
                unitPrice = lineSnapshot.unitPrice,
            )
        }
        val relatedSaleLineIdentityBySaleLineId = saleSessionLines.associate { it.identity.id!! to it.identity }
        val saleSessionAdjustments = saleAdjustmentSnapshots.map { adjustmentSnapshot ->
            SaleSessionAdjustment(
                identity = SessionIdentity.persisted(adjustmentSnapshot.id),
                relatedSaleLineIdentity = adjustmentSnapshot.saleLineId?.let { relatedSaleLineIdentityBySaleLineId[it] },
                adjustmentReasonId = adjustmentSnapshot.adjustmentReasonId,
                direction = adjustmentSnapshot.direction,
                calculationMethod = adjustmentSnapshot.calculationMethod,
                value = adjustmentSnapshot.value,
                note = adjustmentSnapshot.note,
                approvedById = adjustmentSnapshot.approvedById,
            )
        }
        val saleSessionPayments = salePaymentSnapshots.map { paymentSnapshot ->
            SaleSessionPayment(
                identity = SessionIdentity.persisted(paymentSnapshot.id),
                paymentMethodId = paymentSnapshot.paymentMethodId,
                amount = paymentSnapshot.amount,
                reference = paymentSnapshot.reference,
                paymentDate = paymentSnapshot.paymentDate,
                voidedReason = paymentSnapshot.voidedReason,
            )
        }

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
            header = SaleSessionHeader(
                contactId = saleHeader.contactId,
                soldById = saleHeader.soldById,
                dateSold = saleHeader.dateSold,
                notes = saleHeader.notes,
            ),
            saleLines = saleSessionLines,
            saleAdjustments = saleSessionAdjustments,
            salePayments = saleSessionPayments,
            totals = SaleSessionTotals.ZERO,
        )
        return saleSessionTotalsCalculator.recompute(saleSession)
    }
}
