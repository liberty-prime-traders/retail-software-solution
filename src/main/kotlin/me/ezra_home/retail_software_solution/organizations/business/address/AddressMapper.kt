package me.ezra_home.retail_software_solution.organizations.business.address

import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import me.ezra_home.retail_software_solution.organizations.business.address.dto.AddressDto
import me.ezra_home.retail_software_solution.organizations.business.address.api.AddressInsertDto
import me.ezra_home.retail_software_solution.organizations.business.address.api.AddressResponseDto
import me.ezra_home.retail_software_solution.organizations.business.address.api.AddressUpdateDto
import me.ezra_home.retail_software_solution.organizations.business.address.AddressEntity
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(config = RtsMapperConfig::class)
interface AddressMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toDomainDto(addressInsertDto: AddressInsertDto): AddressDto

    fun toDomainDto(addressEntity: AddressEntity): AddressDto

    fun toEntity(addressDto: AddressDto): AddressEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toDto(addressDto: AddressDto): AddressResponseDto

    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(addressUpdateDto: AddressUpdateDto, @MappingTarget addressDto: AddressDto)
}
