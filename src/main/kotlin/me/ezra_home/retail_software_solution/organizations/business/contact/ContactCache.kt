package me.ezra_home.retail_software_solution.organizations.business.contact

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.organizations.model.ContactEntity
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.CONTACT])
internal class ContactCache(
    private val contactRepository: ContactRepository
) {

    @Cacheable
    fun getAllContacts(): Collection<ContactEntity> {
        return contactRepository.findAll()
    }

    @CacheEvict(allEntries = true)
    fun upsertContact(contactEntity: ContactEntity) {
        contactRepository.save(contactEntity)
    }

    @CacheEvict(allEntries = true)
    fun deleteContact(id: UUID) {
        contactRepository.deleteById(id)
    }
}
