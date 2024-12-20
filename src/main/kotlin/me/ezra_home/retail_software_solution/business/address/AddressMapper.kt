package me.ezra_home.retail_software_solution.business.address

import me.ezra_home.retail_software_solution.business.address.dto.AddressInsertDto
import me.ezra_home.retail_software_solution.business.address.dto.AddressResponseDto
import me.ezra_home.retail_software_solution.business.address.dto.AddressUpdateDto
import me.ezra_home.retail_software_solution.configuration.mapping.RtsMapperConfig
import me.ezra_home.retail_software_solution.business.util.mappers.userinfo.CreatedBy
import me.ezra_home.retail_software_solution.business.util.mappers.userinfo.FullName
import me.ezra_home.retail_software_solution.model.entity.AddressEntity
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
    @Mapping(target = "predecessorOfId", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @BeanMapping(qualifiedBy = [CreatedBy::class])
    fun toEntity(addressInsertDto: AddressInsertDto): AddressEntity

    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "predecessorOfId", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(addressDto: AddressUpdateDto, @MappingTarget addressEntity: AddressEntity)

}
