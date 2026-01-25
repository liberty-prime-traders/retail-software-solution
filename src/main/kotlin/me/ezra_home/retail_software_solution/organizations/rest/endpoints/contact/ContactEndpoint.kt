package me.ezra_home.retail_software_solution.organizations.rest.endpoints.contact

import me.ezra_home.retail_software_solution.organizations.business.contact.ContactService
import me.ezra_home.retail_software_solution.organizations.business.contact.dto.ContactInsertDto
import me.ezra_home.retail_software_solution.organizations.business.contact.dto.ContactResponseDto
import me.ezra_home.retail_software_solution.organizations.business.contact.dto.ContactUpdateDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
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

    @DeleteMapping("{id}")
    fun deleteContact(@PathVariable id: UUID): ResponseEntity<HttpStatus> {
        contactService.deleteContact(id)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
