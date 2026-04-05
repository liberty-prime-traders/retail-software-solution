package me.ezra_home.retail_software_solution.organizations.business.payment_method

import me.ezra_home.retail_software_solution.organizations.business.payment_method.dto.PaymentMethodInsertDto
import me.ezra_home.retail_software_solution.organizations.business.payment_method.dto.PaymentMethodResponseDto
import me.ezra_home.retail_software_solution.organizations.business.payment_method.dto.PaymentMethodUpdateDto
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import me.ezra_home.retail_software_solution.organizations.model.PaymentMethodEntity
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(config = RtsMapperConfig::class)
internal interface PaymentMethodMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(paymentMethodInsertDto: PaymentMethodInsertDto): PaymentMethodEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toResponseDto(paymentMethodEntity: PaymentMethodEntity): PaymentMethodResponseDto

    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(paymentMethodUpdateDto: PaymentMethodUpdateDto, @MappingTarget paymentMethodEntity: PaymentMethodEntity)
}
