package me.ezra_home.retail_software_solution.locations.business.supplier_payment

import me.ezra_home.retail_software_solution.locations.business.supplier_payment.api.SupplierPaymentVoidCreateDto
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.api.SupplierPaymentVoidDto
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface SupplierPaymentVoidMapper {

    fun toDomainDto(entity: SupplierPaymentVoidEntity): SupplierPaymentVoidDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    fun toEntity(dto: SupplierPaymentVoidCreateDto): SupplierPaymentVoidEntity
}
