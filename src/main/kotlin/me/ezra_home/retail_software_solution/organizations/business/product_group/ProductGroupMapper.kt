package me.ezra_home.retail_software_solution.organizations.business.product_group

import me.ezra_home.retail_software_solution.organizations.business.product_group.api.ProductGroupDto
import me.ezra_home.retail_software_solution.organizations.business.product_group.api.ProductGroupInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product_group.api.ProductGroupResponseDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface ProductGroupMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(productGroupInsertDto: ProductGroupInsertDto): ProductGroupEntity

    fun toDomainDto(productGroupEntity: ProductGroupEntity): ProductGroupDto

    fun toEntity(productGroupDto: ProductGroupDto): ProductGroupEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    @Mapping(target = "categoryName", expression = "java(categoryName)")
    @Mapping(source = "categoryId", target = "categoryId")
    fun toResponseDto(productGroupDto: ProductGroupDto, @Context categoryName: String?): ProductGroupResponseDto
}
