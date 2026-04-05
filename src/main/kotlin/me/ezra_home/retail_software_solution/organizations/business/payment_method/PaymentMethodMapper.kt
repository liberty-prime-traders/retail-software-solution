package me.ezra_home.retail_software_solution.organizations.business.payment_method

import me.ezra_home.retail_software_solution.organizations.business.payment_method.dto.PaymentMethodDto
import me.ezra_home.retail_software_solution.organizations.business.payment_method.public.PaymentMethodInsertDto
import me.ezra_home.retail_software_solution.organizations.business.payment_method.public.PaymentMethodResponseDto
import me.ezra_home.retail_software_solution.organizations.business.payment_method.public.PaymentMethodUpdateDto
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import me.ezra_home.retail_software_solution.organizations.business.payment_method.PaymentMethodEntity
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
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
    @Mapping(target = "referenceNumber", ignore = true)
    fun toDomainDto(paymentMethodInsertDto: PaymentMethodInsertDto): PaymentMethodDto

    fun toDomainDto(paymentMethodEntity: PaymentMethodEntity): PaymentMethodDto

    fun toEntity(paymentMethodDto: PaymentMethodDto): PaymentMethodEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    fun toResponseDto(paymentMethodDto: PaymentMethodDto): PaymentMethodResponseDto

    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(paymentMethodUpdateDto: PaymentMethodUpdateDto, @MappingTarget paymentMethodDto: PaymentMethodDto)
}
