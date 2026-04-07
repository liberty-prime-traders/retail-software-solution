package me.ezra_home.retail_software_solution.organizations.business.address

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.organizations.business.address.api.AddressInsertDto
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
    fun create(insertDto: AddressInsertDto): AddressDto {
        val saved = addressRepository.saveAndFlush(addressMapper.toEntity(insertDto))
        return addressMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun save(addressDto: AddressDto): AddressDto {
        val saved = addressRepository.save(addressMapper.toEntity(addressDto))
        return addressMapper.toDomainDto(saved)
    }
}
