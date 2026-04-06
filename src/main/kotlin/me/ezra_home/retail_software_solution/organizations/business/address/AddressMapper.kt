package me.ezra_home.retail_software_solution.organizations.business.address

import me.ezra_home.retail_software_solution.organizations.business.address.api.AddressInsertDto
import me.ezra_home.retail_software_solution.organizations.business.address.api.AddressResponseDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface AddressMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(addressInsertDto: AddressInsertDto): AddressEntity

    fun toDomainDto(addressEntity: AddressEntity): AddressDto

    fun toEntity(addressDto: AddressDto): AddressEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toDto(addressDto: AddressDto): AddressResponseDto
}
