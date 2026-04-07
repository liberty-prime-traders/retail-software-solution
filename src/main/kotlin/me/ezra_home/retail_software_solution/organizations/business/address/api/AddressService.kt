package me.ezra_home.retail_software_solution.organizations.business.address.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.organizations.business.address.AddressCache
import me.ezra_home.retail_software_solution.organizations.business.address.AddressMapper
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.Objects

@Service
@TransactionalOnLocationSchema
class AddressService(
    private val addressMapper: AddressMapper,
    private val addressCache: AddressCache
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun getAllAddresses(): Collection<AddressResponseDto> {
        return addressCache.getAllAddresses().map { addressMapper.toDto(it) }
    }

    fun createAddress(addressInsertDto: AddressInsertDto): AddressResponseDto {
        val dto = addressCache.create(addressInsertDto)
        return addressMapper.toDto(dto)
    }

    fun updateAddress(addressUpdateDto: AddressUpdateDto): AddressResponseDto {
        val existing = addressCache.getAllAddresses().find { Objects.equals(addressUpdateDto.id, it.id) }
            ?: throw UpdatingNonExistingRecordException()
        val updated = addressUpdateDto.applyTo(existing)
        val saved = addressCache.save(updated)
        return addressMapper.toDto(saved)
    }
}
