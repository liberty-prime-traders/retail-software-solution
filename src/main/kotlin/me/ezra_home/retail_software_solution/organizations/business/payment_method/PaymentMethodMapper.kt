package me.ezra_home.retail_software_solution.organizations.business.payment_method

import me.ezra_home.retail_software_solution.organizations.business.payment_method.api.PaymentMethodInsertDto
import me.ezra_home.retail_software_solution.organizations.business.payment_method.api.PaymentMethodResponseDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface PaymentMethodMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(paymentMethodInsertDto: PaymentMethodInsertDto): PaymentMethodEntity

    fun toDomainDto(paymentMethodEntity: PaymentMethodEntity): PaymentMethodDto

    fun toEntity(paymentMethodDto: PaymentMethodDto): PaymentMethodEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    @Mapping(target = "linkedAccount", expression = "java(linkedAccount)")
    fun toResponseDto(paymentMethodDto: PaymentMethodDto, @Context linkedAccount: String?): PaymentMethodResponseDto
}
