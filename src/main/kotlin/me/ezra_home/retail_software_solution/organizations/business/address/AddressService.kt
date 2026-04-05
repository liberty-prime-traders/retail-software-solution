package me.ezra_home.retail_software_solution.organizations.business.address

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.organizations.business.address.dto.AddressInsertDto
import me.ezra_home.retail_software_solution.organizations.business.address.dto.AddressResponseDto
import me.ezra_home.retail_software_solution.organizations.business.address.dto.AddressUpdateDto
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.Objects

@Service
@TransactionalOnLocationSchema
class AddressService internal constructor(
    private val addressMapper: AddressMapper,
    private val addressCache: AddressCache
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun getAllAddresses(): Collection<AddressResponseDto> {
        return addressCache.getAllAddresses().map { addressMapper.toDto(it) }
    }

    fun createAddress(addressInsertDto: AddressInsertDto): AddressResponseDto {
        val dto = addressMapper.toDomainDto(addressInsertDto)
        addressCache.upsertAddress(dto)
        return addressMapper.toDto(dto)
    }

    fun updateAddress(addressUpdateDto: AddressUpdateDto): AddressResponseDto {
        val dto = addressCache.getAllAddresses().find { Objects.equals(addressUpdateDto.id, it.id) }
            ?: throw UpdatingNonExistingRecordException()
        addressMapper.partialUpdate(addressUpdateDto, dto)
        addressCache.upsertAddress(dto)
        return addressMapper.toDto(dto)
    }
}
