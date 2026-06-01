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
    private val saleSessionUpdateFinalizer: SaleSessionUpdateFinalizer,
) {

    fun update(sessionId: UUID, headerUpdateDto: SaleSessionHeaderUpdateDto): SaleSessionResponseDto {
        val session = saleSessionStore.load(sessionId)
        saleSessionValidator.guardMutable(session)
        val updatedHeader = session.header.copy(
            contactId = headerUpdateDto.contactId?.let {
                it.orElseThrow { RtsGenericException("contactId cannot be cleared") }
            } ?: session.header.contactId,
            soldById = headerUpdateDto.soldById?.orElse(null) ?: session.header.soldById,
            dateSold = headerUpdateDto.dateSold?.orElse(null) ?: session.header.dateSold,
            notes = StringUtils.useIfProvided(headerUpdateDto.notes, session.header.notes),
        )
        if (updatedHeader.contactId != SystemContact.WALK_IN.id) {
            contactService.guardExists(updatedHeader.contactId)
        }
        return saleSessionUpdateFinalizer.finalize(session.copy(header = updatedHeader))
    }
}
