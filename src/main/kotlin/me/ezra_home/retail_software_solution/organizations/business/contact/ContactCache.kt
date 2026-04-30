package me.ezra_home.retail_software_solution.organizations.business.contact

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactInsertDto
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
    fun create(insertDto: ContactInsertDto): ContactDto {
        val saved = contactRepository.saveAndFlush(contactMapper.toEntity(insertDto))
        return contactMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun save(contactDto: ContactDto): ContactDto {
        val saved = contactRepository.save(contactMapper.toEntity(contactDto))
        return contactMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun save(contactEntity: ContactEntity) {
        contactRepository.save(contactEntity)
    }

    @CacheEvict(allEntries = true)
    fun deleteContact(id: UUID) {
        contactRepository.deleteById(id)
    }
}
