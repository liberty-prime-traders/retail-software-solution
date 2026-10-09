package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerSourceType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component

@Component
class LedgerPostingGate(
    private val ledgerEntryGroupRepository: LedgerEntryGroupRepository
) {

    fun isPosted(sourceContext: EventSourceContext, reference: String, sourceType: LedgerSourceType): Boolean =
        when (sourceContext) {
            is EventSourceContext.OrgLevel ->
                ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceTypeAndSourceLocationIdIsNull(reference, sourceType)
            is EventSourceContext.LocationLevel ->
                ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceTypeAndSourceLocationId(
                    reference, sourceType, requireSessionLocationId(sourceContext)
                )
        }

    // A void must fail loudly rather than be skipped: a skipped void is never revisited once the original posts on retry.
    @TransactionalOnOrganizationSchema(readOnly = true)
    fun requirePosted(sourceContext: EventSourceContext, reference: String, originalSourceType: LedgerSourceType) {
        if (!isPosted(sourceContext, reference, originalSourceType)) {
            throw RtsGenericException("Cannot post the void of $reference: its ${originalSourceType.name} posting does not exist")
        }
    }

    // Read in the session the consumer initialised from the event, so the session tier is the event's tier.
    fun idempotencyConstraintName(): String =
        if (SessionContextProvider.getLocationIdOrNull() == null) "uq_ledger_entry_grp_ref_type_org" else "uq_ledger_entry_grp_ref_type_loc"

    // A mismatched session would key the existence check to another location and let a duplicate posting through.
    private fun requireSessionLocationId(locationLevel: EventSourceContext.LocationLevel) =
        SessionContextProvider.getLocationIdOrNull()
            ?.takeIf { SessionContextProvider.getLocationSchema() == locationLevel.locationSchema }
            ?: throw RtsGenericException("Location-level event for ${locationLevel.locationSchema} processed outside that location's session")
}
