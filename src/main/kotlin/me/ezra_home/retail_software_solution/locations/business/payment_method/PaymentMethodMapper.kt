package me.ezra_home.retail_software_solution.locations.business.payment_method

import me.ezra_home.retail_software_solution.locations.business.payment_method.dto.PaymentMethodInsertDto
import me.ezra_home.retail_software_solution.locations.business.payment_method.dto.PaymentMethodResponseDto
import me.ezra_home.retail_software_solution.locations.business.payment_method.dto.PaymentMethodUpdateDto
import me.ezra_home.retail_software_solution.configuration.mapping.RtsMapperConfig
import me.ezra_home.retail_software_solution.locations.model.PaymentMethodEntity
import me.ezra_home.retail_software_solution.util.business.mappers.userinfo.CreatedBy
import me.ezra_home.retail_software_solution.util.business.mappers.userinfo.FullName
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(config = RtsMapperConfig::class)
interface PaymentMethodMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "predecessorOfId", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @BeanMapping(qualifiedBy = [CreatedBy::class])
    fun toEntity(paymentMethodInsertDto: PaymentMethodInsertDto): PaymentMethodEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toResponseDto(paymentMethodEntity: PaymentMethodEntity): PaymentMethodResponseDto

    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "predecessorOfId", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(paymentMethodUpdateDto: PaymentMethodUpdateDto, @MappingTarget paymentMethodEntity: PaymentMethodEntity)
}
