package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactInsertDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactLedgerSearchParameters
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactResponseDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactUpdateDto
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerEntryResponse
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/contacts")
class ContactEndpoint(private val contactService: ContactService) {

    @GetMapping
    fun getAllContacts(): Collection<ContactResponseDto> = contactService.getAllContacts()

    @PostMapping
    fun createContact(@RequestBody contactInsertDto: ContactInsertDto): ContactResponseDto =
        contactService.createContact(contactInsertDto)

    @PutMapping
    fun updateContact(@RequestBody contactUpdateDto: ContactUpdateDto): ContactResponseDto =
        contactService.updateContact(contactUpdateDto)

    @PostMapping("{id}/ledger")
    fun getLedger(
        @PathVariable id: UUID,
        @RequestBody pageRequest: PageRequest<ContactLedgerSearchParameters, String>
    ): PageResponse<SubledgerEntryResponse, String> = contactService.getLedger(id, pageRequest)
}
