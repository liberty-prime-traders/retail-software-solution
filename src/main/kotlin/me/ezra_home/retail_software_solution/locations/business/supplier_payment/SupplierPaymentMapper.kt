package me.ezra_home.retail_software_solution.locations.business.supplier_payment

import me.ezra_home.retail_software_solution.locations.business.supplier_payment.api.SupplierPaymentCreateDto
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.api.SupplierPaymentDto
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface SupplierPaymentMapper {

    fun toDomainDto(entity: SupplierPaymentEntity): SupplierPaymentDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    fun toEntity(dto: SupplierPaymentCreateDto): SupplierPaymentEntity
}
