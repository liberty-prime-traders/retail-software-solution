package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionResponseDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionSummaryDto
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentReasonService
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.SystemAdjustmentReason
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.payment_method.api.PaymentMethodService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.enums.SystemContact
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

@Component
class SaleSessionAssembler(
    private val contactService: ContactService,
    private val paymentMethodService: PaymentMethodService,
    private val adjustmentReasonService: AdjustmentReasonService,
    private val saleSessionTotalsCalculator: SaleSessionTotalsCalculator,
    private val sessionToResponseMapper: SessionToResponseMapper,
) {

    private val activeWarningSeconds: Long = 300

    fun buildResponse(saleSession: SaleSession): SaleSessionResponseDto {
        val sessionMappingContext = SaleSessionMappingContext(
            contactLabel = contactNameFor(saleSession.header.contactId),
            walkInCustomer = saleSession.header.contactId == SystemContact.WALK_IN.id,
            showActiveUserWarning = showActiveUserWarning(saleSession),
            showUnreservedChangesWarning = saleSession.showUnreservedChangesWarning
        )
        val adjustmentMappingContext = AdjustmentMappingContext(
            adjustmentReasonNamesById = adjustmentReasonService.getReasonNamesById(),
            saleSessionTotalsCalculator = saleSessionTotalsCalculator,
            saleSessionLines = saleSession.saleLines,
        )
        val lineMappingContext = buildLineMappingContext(saleSession)
        return sessionToResponseMapper.toResponseDto(
            saleSession,
            sessionMappingContext,
            adjustmentMappingContext,
            lineMappingContext,
            paymentMethodService.getNamesById(),
        )
    }

    private fun buildLineMappingContext(saleSession: SaleSession): LineMappingContext {
        val priceOverrideReasonId = adjustmentReasonService.getSystemReasonId(SystemAdjustmentReason.PRICE_OVERRIDE)
        val saleSessionLinesByKey = saleSession.saleLines.associateBy { it.identity.key() }

        val unitPriceOverrideAdjustmentsByLineKey = saleSession.saleAdjustments
            .filter { it.isPriceOverride(priceOverrideReasonId) }
            .associateBy { it.relatedSaleLineIdentity!!.key() }

        val unitPriceOverrideByLineKey = saleSessionLinesByKey.mapValues { (lineKey, saleSessionLine) ->
            val overrideAdjustment = unitPriceOverrideAdjustmentsByLineKey[lineKey]
            overrideAdjustment?.let {
                when (it.direction) {
                    AdjustmentDirection.DISCOUNT -> saleSessionLine.unitPrice - it.value
                    AdjustmentDirection.SURCHARGE -> saleSessionLine.unitPrice + it.value
                    AdjustmentDirection.BOTH -> null
                }
            } ?: saleSessionLine.unitPrice
        }

        val lineAdjustmentsByLineKey = saleSession.saleAdjustments
            .filter { it.relatedSaleLineIdentity != null }
            .groupBy { it.relatedSaleLineIdentity!!.key() }

        val netUnitPriceByLineKey = saleSessionLinesByKey.mapValues { (lineKey, saleSessionLine) ->
            val lineAdjustments = lineAdjustmentsByLineKey[lineKey] ?: emptyList()
            val netAdjustmentAmount = lineAdjustments.sumOf { saleSessionAdjustment ->
                val calculatedAmount = saleSessionTotalsCalculator.calculatedAmount(saleSessionAdjustment, saleSession.saleLines)
                when (saleSessionAdjustment.direction) {
                    AdjustmentDirection.DISCOUNT -> calculatedAmount.negate()
                    AdjustmentDirection.SURCHARGE -> calculatedAmount
                    AdjustmentDirection.BOTH -> calculatedAmount.negate()
                }
            }
            Decimals.divideScale4(saleSessionLine.lineTotal + netAdjustmentAmount, saleSessionLine.quantity)
        }

        return LineMappingContext(
            unitPriceOverrideByLineKey = unitPriceOverrideByLineKey,
            netUnitPriceByLineKey = netUnitPriceByLineKey,
        )
    }

    private fun showActiveUserWarning(saleSession: SaleSession): Boolean {
        if (saleSession.lastAccessedById == SessionContextProvider.getUserId()) return false
        val now = DateTimes.Offset.Now.organization()
        val warningThreshold = Duration.ofSeconds(activeWarningSeconds)
        return Duration.between(saleSession.lastAccessedAt, now) <= warningThreshold
    }

    fun buildSummaries(saleSessions: List<SaleSession>): List<SaleSessionSummaryDto> {
        if (saleSessions.isEmpty()) return emptyList()
        return saleSessions.map { session ->
            sessionToResponseMapper.toSummaryDto(session, contactNameFor(session.header.contactId))
        }
    }

    private fun contactNameFor(contactId: UUID): String {
        if (contactId == SystemContact.WALK_IN.id) return "Walk-In Customer"
        return contactService.getContactById(contactId).identity.displayName
    }
}
