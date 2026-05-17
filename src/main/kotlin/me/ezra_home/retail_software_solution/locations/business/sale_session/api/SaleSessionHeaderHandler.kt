package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionAssembler
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionStore
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionTotalsCalculator
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionValidator
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.enums.SystemContact
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
class SaleSessionHeaderHandler(
    private val saleSessionStore: SaleSessionStore,
    private val saleSessionAssembler: SaleSessionAssembler,
    private val saleSessionValidator: SaleSessionValidator,
    private val saleSessionTotalsCalculator: SaleSessionTotalsCalculator,
    private val contactService: ContactService,
) {

    fun update(sessionId: String, dto: SaleSessionHeaderUpdateDto): SaleSessionResponseDto {
        val session = saleSessionStore.load(sessionId)
        val newHeader = session.header.copy(
            contactId = dto.contactId?.let {
                it.orElseThrow { RtsGenericException("contactId cannot be cleared") }
            } ?: session.header.contactId,
            soldById = dto.soldById?.let { it.orElse(null) } ?: session.header.soldById,
            dateSold = dto.dateSold?.let { it.orElse(null) } ?: session.header.dateSold,
            notes = dto.notes?.let { it.orElse(null) } ?: session.header.notes,
        )
        if (newHeader.contactId != SystemContact.WALK_IN.id) {
            contactService.guardExists(newHeader.contactId)
        }
        val now = DateTimes.Offset.Now.organization()
        val touched = session.copy(header = newHeader).touched(SessionContextProvider.getUserId(), now)
        val withTotals = saleSessionTotalsCalculator.recompute(touched)
        saleSessionValidator.validate(withTotals)
        saleSessionStore.save(withTotals)
        return saleSessionAssembler.buildResponse(withTotals)
    }
}
