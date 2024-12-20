package me.ezra_home.retail_software_solution.business.address

import me.ezra_home.retail_software_solution.business.address.dto.AddressInsertDto
import me.ezra_home.retail_software_solution.business.address.dto.AddressResponseDto
import me.ezra_home.retail_software_solution.business.address.dto.AddressUpdateDto
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.Objects

@Service
class AddressService(
    private val addressMapper: AddressMapper,
    private val addressCache: AddressCache
) {

    @Transactional
    fun getAllAddresses(): Collection<AddressResponseDto> {
        return addressCache.getAllAddresses().map { addressMapper.toDto(it) }
    }

    @Transactional
    fun createAddress(addressInsertDto: AddressInsertDto): AddressResponseDto {
        val newAddressEntity = addressMapper.toEntity(addressInsertDto)
        val savedAddressEntity = addressCache.upsertAddress(newAddressEntity)
        return addressMapper.toDto(savedAddressEntity)
    }

    @Transactional
    fun updateAddress(addressDto: AddressUpdateDto): AddressResponseDto {
        val addressToUpdate = addressCache.getAllAddresses().find { Objects.equals(addressDto.getIdFromOptional(), it.id) }
        if (addressToUpdate == null) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Attempted to Update non-existent record")
        addressMapper.partialUpdate(addressDto, addressToUpdate)
        val updatedAddress = addressCache.upsertAddress(addressToUpdate)
        return addressMapper.toDto(updatedAddress)
    }
}
