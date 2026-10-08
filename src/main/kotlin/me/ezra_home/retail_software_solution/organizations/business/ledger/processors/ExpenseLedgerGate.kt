package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerSourceType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component

@Component
class ExpenseLedgerGate(
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

    // Read after dispatch in the session the consumer initialised from the event, so the session tier is the event's tier.
    fun idempotencyConstraintName(): String =
        if (SessionContextProvider.getLocationIdOrNull() == null) "uq_ledger_entry_grp_ref_type_org" else "uq_ledger_entry_grp_ref_type_loc"

    // A mismatched session would key the existence check to another location and let a duplicate posting through.
    private fun requireSessionLocationId(locationLevel: EventSourceContext.LocationLevel) =
        SessionContextProvider.getLocationIdOrNull()
            ?.takeIf { SessionContextProvider.getLocationSchema() == locationLevel.locationSchema }
            ?: throw RtsGenericException("Location-level expense event for ${locationLevel.locationSchema} processed outside that location's session")
}
