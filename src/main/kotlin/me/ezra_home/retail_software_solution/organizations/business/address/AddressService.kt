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
class AddressService(
    private val addressMapper: AddressMapper,
    private val addressCache: AddressCache
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun getAllAddresses(): Collection<AddressResponseDto> {
        return addressCache.getAllAddresses().map { addressMapper.toDto(it) }
    }

    fun createAddress(addressInsertDto: AddressInsertDto): AddressResponseDto {
        val newAddressEntity = addressMapper.toEntity(addressInsertDto)
        val savedAddressEntity = addressCache.upsertAddress(newAddressEntity)
        return addressMapper.toDto(savedAddressEntity)
    }

    fun updateAddress(addressDto: AddressUpdateDto): AddressResponseDto {
        val addressToUpdate = addressCache.getAllAddresses().find { Objects.equals(addressDto.id, it.id) }
        if (addressToUpdate == null) throw UpdatingNonExistingRecordException()
        addressMapper.partialUpdate(addressDto, addressToUpdate)
        val updatedAddress = addressCache.upsertAddress(addressToUpdate)
        return addressMapper.toDto(updatedAddress)
    }
}
