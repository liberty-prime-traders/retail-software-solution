package me.ezra_home.retail_software_solution.organizations.business.address

import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import me.ezra_home.retail_software_solution.organizations.business.address.dto.AddressInsertDto
import me.ezra_home.retail_software_solution.organizations.business.address.dto.AddressResponseDto
import me.ezra_home.retail_software_solution.organizations.business.address.dto.AddressUpdateDto
import me.ezra_home.retail_software_solution.organizations.model.AddressEntity
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(config = RtsMapperConfig::class)
interface AddressMapper {
    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toDto(addressEntity: AddressEntity): AddressResponseDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(addressInsertDto: AddressInsertDto): AddressEntity

    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(addressDto: AddressUpdateDto, @MappingTarget addressEntity: AddressEntity)
}
