package me.ezra_home.retail_software_solution.organizations.business.contact

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.organizations.business.contact.dto.ContactDto
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.CONTACT])
class ContactCache(
    private val contactRepository: ContactRepository,
    private val contactMapper: ContactMapper
) {

    @Cacheable
    fun getAllContacts(): Collection<ContactDto> {
        return contactRepository.findAll().map { contactMapper.toDomainDto(it) }
    }

    @CacheEvict(allEntries = true)
    fun upsertContact(contactDto: ContactDto) {
        contactRepository.save(contactMapper.toEntity(contactDto))
    }

    @CacheEvict(allEntries = true)
    fun deleteContact(id: UUID) {
        contactRepository.deleteById(id)
    }
}
