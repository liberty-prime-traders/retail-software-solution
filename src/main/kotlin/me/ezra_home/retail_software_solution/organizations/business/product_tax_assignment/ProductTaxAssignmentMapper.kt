package me.ezra_home.retail_software_solution.organizations.business.product_tax_assignment

import me.ezra_home.retail_software_solution.organizations.business.product_tax_assignment.dto.ProductTaxAssignmentInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product_tax_assignment.dto.ProductTaxAssignmentResponseDto
import me.ezra_home.retail_software_solution.organizations.model.ProductTaxAssignmentEntity
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface ProductTaxAssignmentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(dto: ProductTaxAssignmentInsertDto): ProductTaxAssignmentEntity

    fun toResponseDto(entity: ProductTaxAssignmentEntity): ProductTaxAssignmentResponseDto
}
