package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale_session.api.ActiveSessionUser
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionResponseDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionSummaryDto
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentReasonService
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.payment_method.api.PaymentMethodService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import me.ezra_home.retail_software_solution.util.enums.SystemContact
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

@Component
class SaleSessionAssembler(
    private val contactService: ContactService,
    private val userQualifier: UserQualifier,
    private val paymentMethodService: PaymentMethodService,
    private val adjustmentReasonService: AdjustmentReasonService,
    private val saleSessionTotalsCalculator: SaleSessionTotalsCalculator,
    private val saleSessionMapper: SaleSessionMapper,
) {

    private val activeWarningSeconds: Long = 300

    fun buildResponse(session: SaleSession): SaleSessionResponseDto {
        val ctx = SaleSessionMappingContext(
            contactLabel = contactNameFor(session.header.contactId),
            walkInCustomer = session.header.contactId == SystemContact.WALK_IN.id,
        )
        val adjustmentCtx = AdjustmentMappingContext(
            reasonNamesById = adjustmentReasonService.getReasonNamesById(),
            totalsCalculator = saleSessionTotalsCalculator,
            lines = session.lines,
        )
        return saleSessionMapper.toResponseDto(
            session,
            ctx,
            adjustmentCtx,
            paymentMethodService.getNamesById(),
        )
    }

    fun buildSummaries(sessions: List<SaleSession>): List<SaleSessionSummaryDto> {
        if (sessions.isEmpty()) return emptyList()
        val now = DateTimes.Offset.Now.organization()
        val warning = Duration.ofSeconds(activeWarningSeconds)
        return sessions.map { session ->
            val activeUser = if (Duration.between(session.lastAccessedAt, now) <= warning) {
                ActiveSessionUser(
                    userId = session.lastAccessedById,
                    userLabel = userQualifier.getUserFullName(session.lastAccessedById),
                    lastSeenAt = session.lastAccessedAt,
                )
            } else null
            saleSessionMapper.toSummaryDto(
                session,
                SaleSessionSummaryContext(
                    contactLabel = contactNameFor(session.header.contactId),
                    activeUser = activeUser,
                ),
            )
        }
    }

    private fun contactNameFor(contactId: UUID): String {
        if (contactId == SystemContact.WALK_IN.id) return "Walk-In Customer"
        return contactService.getContactById(contactId).identity.displayName
    }
}
