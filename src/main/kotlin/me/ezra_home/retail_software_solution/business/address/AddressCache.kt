package me.ezra_home.retail_software_solution.business.address

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.model.entity.AddressEntity
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
@CacheConfig(cacheNames = [CacheNames.ADDRESS])
class AddressCache(private val addressRepository: AddressRepository) {

    @Cacheable
    fun getAllAddresses(): Collection<AddressEntity> = addressRepository.findAll()

    @CacheEvict(allEntries = true)
    fun upsertAddress(addressEntity: AddressEntity): AddressEntity = addressRepository.save(addressEntity)
}
