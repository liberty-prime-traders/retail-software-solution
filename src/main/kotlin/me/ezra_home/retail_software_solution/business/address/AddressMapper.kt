package me.ezra_home.retail_software_solution.business.address

import me.ezra_home.retail_software_solution.business.util.mapper.RtsMapperConfig
import me.ezra_home.retail_software_solution.model.entity.AddressEntity
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(config = RtsMapperConfig::class)
interface AddressMapper {
    fun toDto(addressEntity: AddressEntity): AddressDto
    fun toEntity(addressDto: AddressDto): AddressEntity

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(addressDto: AddressDto, @MappingTarget addressEntity: AddressEntity)
}
