package me.ezra_home.retail_software_solution.business.address

import me.ezra_home.retail_software_solution.business.audit.AuditMapper
import me.ezra_home.retail_software_solution.business.audit.AuditService
import me.ezra_home.retail_software_solution.model.util.TableNames
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.Objects

@Service
class AddressService(
    private val addressMapper: AddressMapper,
    private val addressCache: AddressCache,
    private val auditService: AuditService,
    private val auditMapper: AuditMapper
) {

    fun getAllAddresses(): Collection<AddressWithAudit> {
        val auditsForAddress = auditService.getAuditsForTable(TableNames.ADDRESS)
        return addressCache.getAllAddresses().map { address ->
            val auditForAddress = auditsForAddress.find { Objects.equals(it.id?.entityId, address.id) }
            AddressWithAudit(
                auditMapper.toDto(auditForAddress!!),
                addressMapper.toDto(address)
            )
        }
    }

    fun createAddress(addressDto: AddressDto): AddressDto {
        val newAddressEntity = addressMapper.toEntity(addressDto)
        val savedAddressEntity = addressCache.upsertAddress(newAddressEntity)
        return addressMapper.toDto(savedAddressEntity)
    }

    fun updateAddress(addressDto: AddressDto): AddressDto {
        val addressToUpdate = addressCache.getAllAddresses().find { Objects.equals(addressDto.id, it.id) }
        if (addressToUpdate == null) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Attempted to Update non-existent record")
        addressMapper.partialUpdate(addressDto, addressToUpdate)
        val updatedAddress = addressCache.upsertAddress(addressToUpdate)
        return addressMapper.toDto(updatedAddress)
    }
}
