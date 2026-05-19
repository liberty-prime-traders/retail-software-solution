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
        val session = SaleSession(
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
            lines = emptyList(),
            adjustments = emptyList(),
            payments = emptyList(),
            totals = SaleSessionTotals.ZERO,
        )
        return saleSessionTotalsCalculator.recompute(session)
    }

    fun loadFromSale(sessionId: UUID, saleId: UUID): SaleSession {
        val now = DateTimes.Offset.Now.organization()
        val userId = SessionContextProvider.getUserId()
        val locationId = SessionContextProvider.getLocationId()
        val header = saleDataFetcher.getSaleHeader(saleId)
        val lineSnapshots = saleDataFetcher.getSaleLines(saleId)
        val adjustmentSnapshots = saleAdjustmentFetcher.getAdjustments(saleId)
        val paymentSnapshots = salePaymentFetcher.getPaymentSnapshots(saleId)

        val lines = lineSnapshots.map {
            SaleSessionLine(
                identity = SessionIdentity.persisted(it.id),
                locationProductId = it.locationProductId,
                productLabel = it.productLabel,
                quantity = it.quantity,
                unitId = it.unitId,
                conversionFactor = it.conversionFactor,
                unitPrice = it.unitPrice,
            )
        }
        val identityByLineId = lines.associate { it.identity.id!! to it.identity }
        val adjustments = adjustmentSnapshots.map { snapshot ->
            SaleSessionAdjustment(
                identity = SessionIdentity.persisted(snapshot.id),
                lineIdentity = snapshot.saleLineId?.let { identityByLineId[it] },
                adjustmentReasonId = snapshot.adjustmentReasonId,
                direction = snapshot.direction,
                calculationMethod = snapshot.calculationMethod,
                value = snapshot.value,
                note = snapshot.note,
                approvedById = snapshot.approvedById,
            )
        }
        val payments = paymentSnapshots.map { snapshot ->
            SaleSessionPayment(
                identity = SessionIdentity.persisted(snapshot.id),
                paymentMethodId = snapshot.paymentMethodId,
                amount = snapshot.amount,
                reference = snapshot.reference,
                paymentDate = snapshot.paymentDate,
                voidedReason = snapshot.voidedReason,
            )
        }

        val session = SaleSession(
            sessionId = sessionId,
            locationId = locationId,
            saleId = header.id,
            saleVersion = header.version,
            originalStatus = header.status,
            createdById = userId,
            createdAt = now,
            lastUpdatedAt = now,
            lastAccessedById = userId,
            lastAccessedAt = now,
            header = SaleSessionHeader(
                contactId = header.contactId,
                soldById = header.soldById,
                dateSold = header.dateSold,
                notes = header.notes,
            ),
            lines = lines,
            adjustments = adjustments,
            payments = payments,
            totals = SaleSessionTotals.ZERO,
        )
        return saleSessionTotalsCalculator.recompute(session)
    }
}
