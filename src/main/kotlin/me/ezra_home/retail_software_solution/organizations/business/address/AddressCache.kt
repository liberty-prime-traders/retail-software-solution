package me.ezra_home.retail_software_solution.organizations.business.address

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.organizations.business.address.AddressDto
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
@CacheConfig(cacheNames = [CacheNames.ADDRESS])
class AddressCache(
    private val addressRepository: AddressRepository,
    private val addressMapper: AddressMapper
) {

    @Cacheable
    fun getAllAddresses(): Collection<AddressDto> = addressRepository.findAll().map { addressMapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun upsertAddress(addressDto: AddressDto) {
        addressRepository.save(addressMapper.toEntity(addressDto))
    }
}
