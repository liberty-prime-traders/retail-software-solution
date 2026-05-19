package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionUpdateFinalizer
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionStore
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionValidator
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.enums.SystemContact
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SaleSessionHeaderHandler(
    private val saleSessionStore: SaleSessionStore,
    private val saleSessionValidator: SaleSessionValidator,
    private val contactService: ContactService,
    private val sessionFinalizer: SaleSessionUpdateFinalizer
) {

    fun update(sessionId: UUID, dto: SaleSessionHeaderUpdateDto): SaleSessionResponseDto {
        val session = saleSessionStore.load(sessionId)
        saleSessionValidator.guardMutable(session)
        val newHeader = session.header.copy(
            contactId = dto.contactId?.let {
                it.orElseThrow { RtsGenericException("contactId cannot be cleared") }
            } ?: session.header.contactId,
            soldById = dto.soldById?.orElse(null) ?: session.header.soldById,
            dateSold = dto.dateSold?.orElse(null) ?: session.header.dateSold,
            notes = StringUtils.useIfProvided(dto.notes, session.header.notes)
        )
        if (newHeader.contactId != SystemContact.WALK_IN.id) {
            contactService.guardExists(newHeader.contactId)
        }
        return sessionFinalizer.finalize(session.copy(header = newHeader))
    }
}
